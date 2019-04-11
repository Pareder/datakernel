package io.global.chat.chatroom;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.global.chat.DynamicOTNodeServlet;
import io.global.ot.client.OTDriver;

import static io.global.chat.Utils.createMergedOTSystem;
import static io.global.chat.chatroom.ChatMultiOperation.CODEC;

public final class RoomModule extends AbstractModule {
	public static final String ROOM_PREFIX = "room";

	@Provides
	@Singleton
	DynamicOTNodeServlet<ChatMultiOperation> provideServlet(OTDriver driver) {
		return DynamicOTNodeServlet.create(driver, createMergedOTSystem(), CODEC, ROOM_PREFIX);
	}
}
