package io.global.chat.chatrooms;

import io.global.common.PubKey;

import java.util.Map;
import java.util.Set;

import static io.global.chat.Utils.generateRoomName;

public class ChatRoomsAddRoom implements ChatRoomsOperation {
	private final Set<PubKey> participants;
	private final String roomName = generateRoomName();

	public ChatRoomsAddRoom(Set<PubKey> participants) {
		this.participants = participants;
	}

	@Override
	public void apply(Map<String, Set<PubKey>> chatRooms) {
		chatRooms.put(roomName, participants);
	}
}
