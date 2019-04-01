package io.global.chat.chatroom.participants;

import io.datakernel.ot.OTSystem;
import io.datakernel.ot.OTSystemImpl;
import io.datakernel.ot.OTSystemImpl.SquashFunction;
import io.datakernel.ot.TransformResult;
import io.global.common.PubKey;

import java.util.Set;
import java.util.function.Function;

import static io.datakernel.util.CollectionUtils.difference;
import static io.datakernel.util.CollectionUtils.union;
import static io.global.chat.chatroom.participants.AddParticipants.add;
import static io.global.chat.chatroom.participants.RemoveParticipants.remove;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

public final class ParticipantsOTSystem {
	private ParticipantsOTSystem() {
		throw new AssertionError();
	}

	public static final AbstractParticipantsOperation EMPTY = add(emptySet());

	public static OTSystem<AbstractParticipantsOperation> createOTSystem() {
		return OTSystemImpl.<AbstractParticipantsOperation>create()
				.withTransformFunction(AddParticipants.class, AddParticipants.class, (left, right) -> TransformResult.of(right, left))
				.withTransformFunction(AddParticipants.class, RemoveParticipants.class, (left, right) -> TransformResult.of(right, left))
				.withTransformFunction(RemoveParticipants.class, RemoveParticipants.class, (left, right) -> TransformResult.of(right, left))

				.withSquashFunction(AddParticipants.class, AddParticipants.class, squashSameOperations(AddParticipants::add))
				.withSquashFunction(RemoveParticipants.class, RemoveParticipants.class, squashSameOperations(RemoveParticipants::remove))
				.withSquashFunction(AddParticipants.class, RemoveParticipants.class, squashDifferentOperations(AddParticipants::add, RemoveParticipants::remove))
				.withSquashFunction(RemoveParticipants.class, AddParticipants.class, squashDifferentOperations(RemoveParticipants::remove, AddParticipants::add))

				.withInvertFunction(AddParticipants.class, op -> singletonList(remove(op.getParticipants())))
				.withInvertFunction(RemoveParticipants.class, op -> singletonList(add(op.getParticipants())))

				.withEmptyPredicate(AddParticipants.class, AddParticipants::isEmpty)
				.withEmptyPredicate(RemoveParticipants.class, RemoveParticipants::isEmpty);
	}

	private static <OP extends AbstractParticipantsOperation> SquashFunction<AbstractParticipantsOperation, OP, OP> squashSameOperations(Function<Set<PubKey>, AbstractParticipantsOperation> constructor) {
		return (op1, op2) -> {
			if (op1.isEmpty()) return op2;
			if (op2.isEmpty()) return op1;
			return constructor.apply(union(op1.getParticipants(), op2.getParticipants()));
		};
	}

	private static <OP1 extends AbstractParticipantsOperation, OP2 extends AbstractParticipantsOperation> SquashFunction<AbstractParticipantsOperation, OP1, OP2> squashDifferentOperations(
			Function<Set<PubKey>, AbstractParticipantsOperation> constructor1,
			Function<Set<PubKey>, AbstractParticipantsOperation> constructor2) {
		return (op1, op2) -> {
			if (op1.isEmpty()) return op2;
			if (op2.isEmpty()) return op1;

			Set<PubKey> participants1 = op1.getParticipants();
			Set<PubKey> participants2 = op2.getParticipants();
			if (participants1.equals(participants2)) {
				return EMPTY;
			}
			if (participants1.containsAll(participants2)) {
				return constructor1.apply(difference(participants1, participants2));
			}
			if (participants2.containsAll(participants1)) {
				return constructor2.apply(difference(participants2, participants1));
			}
			return null;
		};
	}

}
