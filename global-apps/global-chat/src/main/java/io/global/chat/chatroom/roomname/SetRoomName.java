package io.global.chat.chatroom.roomname;

import io.datakernel.codec.StructuredCodec;
import io.global.chat.chatroom.ChatRoomOTState;
import io.global.chat.chatroom.ChatRoomOperation;

import static io.datakernel.codec.StructuredCodecs.STRING_CODEC;
import static io.datakernel.codec.StructuredCodecs.object;
import static io.datakernel.util.Preconditions.checkState;

public final class SetRoomName implements ChatRoomOperation {
	public static final StructuredCodec<SetRoomName> CODEC = object(SetRoomName::new,
			"prev", SetRoomName::getPrev, STRING_CODEC,
			"next", SetRoomName::getNext, STRING_CODEC);

	private final String prev;
	private final String next;

	public SetRoomName(String prev, String next) {
		this.prev = prev;
		this.next = next;
	}

	@Override
	public void apply(ChatRoomOTState state) {
		checkState(state.getRoomName().equals(prev));
		state.setRoomName(next);
	}

	public String getPrev() {
		return prev;
	}

	public String getNext() {
		return next;
	}

	public boolean isEmpty() {
		return next.equals(prev);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SetRoomName that = (SetRoomName) o;

		if (!prev.equals(that.prev)) return false;
		if (!next.equals(that.next)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = prev.hashCode();
		result = 31 * result + next.hashCode();
		return result;
	}
}
