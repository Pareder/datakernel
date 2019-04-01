package io.global.chat.chatroom.participants;

import io.datakernel.ot.OTSystem;
import io.datakernel.ot.TransformResult;
import io.datakernel.ot.exceptions.OTTransformException;
import io.global.chat.chatroom.ChatRoomOTState;
import io.global.common.KeyPair;
import io.global.common.PubKey;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static io.datakernel.util.CollectionUtils.*;
import static io.global.chat.chatroom.ChatRoomOTState.ofParticipants;
import static io.global.chat.chatroom.participants.AddParticipants.add;
import static io.global.chat.chatroom.participants.ParticipantsOTSystem.createOTSystem;
import static io.global.chat.chatroom.participants.RemoveParticipants.remove;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class ParticipantsOTSystemTest {
	private static final OTSystem<AbstractParticipantsOperation> SYSTEM = createOTSystem();
	private static final Set<PubKey> INITIAL_PARTICIPANTS = set(newPubKey(), newPubKey(), newPubKey());

	// region adds
	@Test
	public void addsDontCross() {
		test(add(set(newPubKey(), newPubKey())), add(set(newPubKey(), newPubKey())));
	}

	@Test
	public void addsTotallyCross() {
		Set<PubKey> participants = set(newPubKey(), newPubKey());
		test(add(participants), add(participants));
	}

	@Test
	public void addsPartiallyCross() {
		Set<PubKey> participants = set(newPubKey(), newPubKey());
		test(add(union(participants, set(newPubKey()))), add(union(participants, set(newPubKey()))));
	}

	@Test
	public void addsSquash() {
		Set<PubKey> participants = set(newPubKey(), newPubKey());
		testSquash(singletonList(add(participants)), add(participants), add(participants));
		Set<PubKey> additionalParticipants = union(participants, set(newPubKey(), newPubKey()));
		testSquash(singletonList(add(additionalParticipants)), add(additionalParticipants), add(participants));
	}
	// endregion

	// region removes
	@Test
	public void removesDontCross() {
		test(remove(set(first(INITIAL_PARTICIPANTS))), remove(set(getLast(INITIAL_PARTICIPANTS))));
	}

	@Test
	public void removesTotallyCross() {
		test(remove(INITIAL_PARTICIPANTS), remove(INITIAL_PARTICIPANTS));
		test(remove(set(first(INITIAL_PARTICIPANTS))), remove(set(first(INITIAL_PARTICIPANTS))));
	}

	@Test
	public void removesPartiallyCross() {
		Set<PubKey> participants1 = subSet(0, 2);
		Set<PubKey> participants2 = subSet(1, 3);
		assertTrue(hasIntersection(participants1, participants2));
		assertFalse(participants1.containsAll(participants2));
		assertFalse(participants2.containsAll(participants1));
		test(remove(participants1), remove(participants2));
	}

	@Test
	public void removesSquash() {
		// Same rooms
		testSquash(singletonList(add(INITIAL_PARTICIPANTS)), add(INITIAL_PARTICIPANTS), add(INITIAL_PARTICIPANTS));
		testSquash(singletonList(add(INITIAL_PARTICIPANTS)), add(subSet(0, 2)), add(subSet(1, 3)));
		Set<PubKey> left = subSet(0, 1);
		Set<PubKey> right = subSet(2, 3);
		testSquash(singletonList(add(union(left, right))), add(left), add(right));
	}
	// endregion

	// region remove and add
	@Test
	public void removeAndAddDontCross() {
		test(remove(set(first(INITIAL_PARTICIPANTS))), add(set(newPubKey())));
	}

	@Test
	public void addAndRemoveSquash() {
		// No overlaps
		testSquash(null, add(set(newPubKey(), newPubKey())), remove(set(first(INITIAL_PARTICIPANTS))));

		// Totally same
		Set<PubKey> participants = set(newPubKey(), newPubKey());
		testSquash(emptyList(), add(participants), remove(participants));

		Set<PubKey> additional = set(newPubKey(), newPubKey());
		Set<PubKey> additionalParticipants = union(participants, additional);
		// Remove overlaps Add
		testSquash(singletonList(remove(additional)), add(participants), remove(additionalParticipants));

		// Insert overlaps delete
		testSquash(singletonList(add(additional)), add(additionalParticipants), remove(participants));
	}

	// endregion

	private void test(AbstractParticipantsOperation left, AbstractParticipantsOperation right) {
		doTestTransformation(left, right);
		doTestTransformation(right, left);
	}

	private void doTestTransformation(AbstractParticipantsOperation left, AbstractParticipantsOperation right) {
		try {
			ChatRoomOTState stateLeft = ofParticipants(INITIAL_PARTICIPANTS);
			ChatRoomOTState stateRight = ofParticipants(INITIAL_PARTICIPANTS);
			TransformResult<AbstractParticipantsOperation> result = SYSTEM.transform(left, right);

			left.apply(stateLeft);
			result.left.forEach(editorOperation -> editorOperation.apply(stateLeft));

			right.apply(stateRight);
			result.right.forEach(editorOperation -> editorOperation.apply(stateRight));

			assertEquals(stateLeft, stateRight);
		} catch (OTTransformException e) {
			throw new AssertionError(e);
		}
	}

	private void testSquash(@Nullable List<AbstractParticipantsOperation> expectedSquash, AbstractParticipantsOperation first, AbstractParticipantsOperation second) {
		doTestSquash(expectedSquash, first, second);
		doTestSquash(expectedSquash, second, first);
	}

	private void doTestSquash(@Nullable List<AbstractParticipantsOperation> expectedSquash, AbstractParticipantsOperation first, AbstractParticipantsOperation second) {
		List<AbstractParticipantsOperation> ops = asList(first, second);
		List<AbstractParticipantsOperation> actualSquash = SYSTEM.squash(ops);
		if (expectedSquash == null) {
			assertEquals(ops, actualSquash);
		} else {
			assertEquals(expectedSquash, actualSquash);
		}
	}

	private static PubKey newPubKey() {
		return KeyPair.generate().getPubKey();
	}

	private static Set<PubKey> subSet(int from, int to) {
		assert ParticipantsOTSystemTest.INITIAL_PARTICIPANTS.size() >= to;
		assert to >= from;
		HashSet<PubKey> subset = new HashSet<>(to - from);
		Iterator<PubKey> iterator = ParticipantsOTSystemTest.INITIAL_PARTICIPANTS.iterator();
		for (int i = 0; i < to; i++) {
			PubKey next = iterator.next();
			if (i >= from) {
				subset.add(next);
			}
		}
		return subset;
	}

}
