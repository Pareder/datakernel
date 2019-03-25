package io.global.chat.chatrooms;

import io.global.common.PubKey;

import java.util.Map;
import java.util.Set;

public interface ChatRoomsOperation {
	void apply(Map<String, Set<PubKey>> chatRooms);
}
