package io.global.chat.chatrooms;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.datakernel.codec.StructuredCodec;
import io.global.chat.DynamicOTNodeServlet;
import io.global.chat.Utils;
import io.global.ot.client.OTDriver;

public final class ChatRoomsModule extends AbstractModule {
	public static final String REPOSITORY_NAME = "chatRooms";

	@Provides
	@Singleton
	DynamicOTNodeServlet<ChatRoomsOperation> provideServlet(OTDriver driver) {
		StructuredCodec<ChatRoomsOperation> diffCodec = null;        // TODO eduard: fix this
		return DynamicOTNodeServlet.create(driver, Utils.createChatRoomsOTSystem(), diffCodec, REPOSITORY_NAME);
	}

}
