package io.global.chat.chatrooms;

import io.global.common.PubKey;

import java.util.Map;
import java.util.Set;

public class ChatRoomsRemoveParticipants implements ChatRoomsOperation {
	private final String roomName;
	private final Set<PubKey> participants;

	public ChatRoomsRemoveParticipants(String roomName, Set<PubKey> participants) {
		this.roomName = roomName;
		this.participants = participants;
	}

	@Override
	public void apply(Map<String, Set<PubKey>> chatRooms) {
		chatRooms.get(roomName).removeAll(participants);
	}
}
