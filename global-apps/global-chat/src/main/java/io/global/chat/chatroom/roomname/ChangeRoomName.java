package io.global.chat.chatroom.roomname;

import io.datakernel.codec.StructuredCodec;
import io.global.chat.chatroom.ChatRoomOTState;
import io.global.chat.chatroom.ChatRoomOperation;

import static io.datakernel.codec.StructuredCodecs.*;
import static io.datakernel.util.Preconditions.checkState;

public final class ChangeRoomName implements ChatRoomOperation {
	public static final StructuredCodec<ChangeRoomName> CODEC = object(ChangeRoomName::new,
			"prev", ChangeRoomName::getPrev, STRING_CODEC,
			"next", ChangeRoomName::getNext, STRING_CODEC,
			"timestamp", ChangeRoomName::getTimestamp, LONG_CODEC);

	private final String prev;
	private final String next;
	private final long timestamp;

	private ChangeRoomName(String prev, String next, long timestamp) {
		this.prev = prev;
		this.next = next;
		this.timestamp = timestamp;
	}

	public static ChangeRoomName changeName(String prev, String next, long timestamp) {
		return new ChangeRoomName(prev, next, timestamp);
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

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isEmpty() {
		return next.equals(prev);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ChangeRoomName that = (ChangeRoomName) o;

		if (timestamp != that.timestamp) return false;
		if (!prev.equals(that.prev)) return false;
		if (!next.equals(that.next)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = prev.hashCode();
		result = 31 * result + next.hashCode();
		result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "ChangeRoomName{" +
				"prev='" + prev + '\'' +
				", next='" + next + '\'' +
				", timestamp=" + timestamp +
				'}';
	}
}
