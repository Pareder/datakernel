package io.global.chat.chatroom;

public interface ChatRoomOperation {
	void apply(ChatRoomOTState state);

	boolean isEmpty();
}
