package io.global.chat.chatroom.participants;

import io.global.chat.chatroom.ChatRoomOperation;
import io.global.common.PubKey;

import java.util.Set;

public abstract class AbstractParticipantsOperation implements ChatRoomOperation {
	protected final Set<PubKey> participants;

	protected AbstractParticipantsOperation(Set<PubKey> participants) {
		this.participants = participants;
	}

	public Set<PubKey> getParticipants() {
		return participants;
	}

	public boolean isEmpty() {
		return participants.isEmpty();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AbstractParticipantsOperation that = (AbstractParticipantsOperation) o;

		if (!participants.equals(that.participants)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return participants.hashCode();
	}
}
