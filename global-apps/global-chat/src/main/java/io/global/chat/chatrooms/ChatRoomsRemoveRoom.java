package io.global.chat.chatrooms;

import io.global.common.PubKey;

import java.util.Map;
import java.util.Set;

public class ChatRoomsRemoveRoom implements ChatRoomsOperation {
	private final String roomName;

	public ChatRoomsRemoveRoom(String roomName) {
		this.roomName = roomName;
	}

	@Override
	public void apply(Map<String, Set<PubKey>> chatRooms) {
		chatRooms.remove(roomName);
	}

}
