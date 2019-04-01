package io.global.chat.chatroom;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.ot.OTSystem;
import io.global.chat.DynamicOTNodeServlet;
import io.global.ot.client.OTDriver;

import static io.global.chat.Utils.CHAT_ROOM_CODEC;

public final class RoomModule extends AbstractModule {
	public static final String ROOM_PREFIX = "room";

	@Provides
	@Singleton
	RoomInitializerServlet provideRoomInitializerServlet(OTDriver driver) {
		return new RoomInitializerServlet(driver);
	}

	@Provides
	@Singleton
	DynamicOTNodeServlet<ChatRoomOperation> provideServlet(Eventloop eventloop, OTDriver driver) {
		OTSystem<ChatRoomOperation> otSystem = null;
		// TODO eduard: merge MessageOTSystem, ParticipantsOTSystem, RoomNameOTSystem into one OT System
		return DynamicOTNodeServlet.create(eventloop, driver, otSystem, CHAT_ROOM_CODEC, ROOM_PREFIX);
	}
}
