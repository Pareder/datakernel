package io.global.chat;

import io.datakernel.ot.OTSystem;
import io.datakernel.ot.OTSystemImpl;
import io.datakernel.ot.TransformResult;
import io.global.chat.chatrooms.ChatRoomsOperation;
import io.global.chat.friendlist.FriendsListOperation;

import java.security.SecureRandom;
import java.util.Base64;

import static java.util.Collections.singletonList;

public class Utils {
	private Utils() {
		throw new AssertionError();
	}

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	public static OTSystem<FriendsListOperation> createFriendsListOTSystem() {
		return OTSystemImpl.<FriendsListOperation>create()
				.withTransformFunction(FriendsListOperation.class, FriendsListOperation.class, ((left, right) -> TransformResult.of(right, left)))
				.withInvertFunction(FriendsListOperation.class, op -> singletonList(op.invert()));
	}

	public static OTSystem<ChatRoomsOperation> createChatRoomsOTSystem() {
		// TODO eduard: implement system
		return OTSystemImpl.<ChatRoomsOperation>create();
	}

	public static String generateRoomName() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().encodeToString(bytes);
	}

}
