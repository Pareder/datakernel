package io.global.chat.roomlist;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.datakernel.eventloop.Eventloop;
import io.global.chat.DynamicOTNodeServlet;
import io.global.ot.client.OTDriver;

import static io.global.chat.roomlist.RoomListOTSystem.createOTSystem;
import static io.global.chat.roomlist.RoomListOperation.ROOM_LIST_CODEC;

public final class RoomListModule extends AbstractModule {
	public static final String REPOSITORY_NAME = "roomList";

	@Provides
	@Singleton
	DynamicOTNodeServlet<RoomListOperation> provideServlet(Eventloop eventloop, OTDriver driver) {
		return DynamicOTNodeServlet.create(eventloop, driver, createOTSystem(), ROOM_LIST_CODEC, REPOSITORY_NAME);
	}
}
