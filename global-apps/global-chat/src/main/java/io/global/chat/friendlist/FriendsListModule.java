package io.global.chat.friendlist;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.global.chat.DynamicOTNodeServlet;
import io.global.chat.Utils;
import io.global.ot.client.OTDriver;

import static io.global.chat.friendlist.FriendsListOperation.FRIENDS_LIST_CODEC;

public final class FriendsListModule extends AbstractModule {
	public static final String REPOSITORY_NAME = "friendsList";

	@Provides
	@Singleton
	DynamicOTNodeServlet<FriendsListOperation> provideServlet(OTDriver driver) {
		return DynamicOTNodeServlet.create(driver, Utils.createFriendsListOTSystem(), FRIENDS_LIST_CODEC, REPOSITORY_NAME);
	}

}
