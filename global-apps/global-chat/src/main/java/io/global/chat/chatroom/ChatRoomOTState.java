package io.global.chat.chatroom;

import io.datakernel.ot.OTState;
import io.global.chat.chatroom.messages.Message;
import io.global.common.PubKey;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public final class ChatRoomOTState implements OTState<ChatRoomOperation> {
	private String roomName;
	private Set<PubKey> participants;
	private Set<Message> messages;

	public ChatRoomOTState() {
	}

	public static ChatRoomOTState ofParticipants(Set<PubKey> participants) {
		ChatRoomOTState state = new ChatRoomOTState();
		state.init();
		state.participants.addAll(participants);
		return state;
	}

	@Override
	public void init() {
		roomName = "";
		participants = new HashSet<>();
		messages = new TreeSet<>(Comparator.comparingLong(Message::getTimestamp));
	}

	@Override
	public void apply(ChatRoomOperation op) {
		op.apply(this);
	}

	public String getRoomName() {
		return roomName;
	}

	public Set<PubKey> getParticipants() {
		return participants;
	}

	public void addParticipants(Set<PubKey> toAdd) {
		participants.addAll(toAdd);
	}

	public void removeParticipants(Set<PubKey> toRemove) {
		participants.removeAll(toRemove);
	}

	public void addMessage(Message message) {
		messages.add(message);
	}

	public void removeMessage(Message message) {
		messages.remove(message);
	}

	public void setRoomName(String newRoomName) {
		this.roomName = newRoomName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ChatRoomOTState that = (ChatRoomOTState) o;

		if (!roomName.equals(that.roomName)) return false;
		if (!participants.equals(that.participants)) return false;
		if (!messages.equals(that.messages)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = roomName.hashCode();
		result = 31 * result + participants.hashCode();
		result = 31 * result + messages.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ChatRoomOTState{" +
				"roomName='" + roomName + '\'' +
				", participants=" + participants +
				", messages=" + messages +
				'}';
	}
}
