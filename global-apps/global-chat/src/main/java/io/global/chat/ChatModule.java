package io.global.chat;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.datakernel.config.Config;
import io.datakernel.dns.AsyncDnsClient;
import io.datakernel.dns.CachedAsyncDnsClient;
import io.datakernel.dns.DnsCache;
import io.datakernel.dns.RemoteAsyncDnsClient;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.http.AsyncHttpClient;
import io.datakernel.http.AsyncHttpServer;
import io.datakernel.http.IAsyncHttpClient;
import io.datakernel.http.MiddlewareServlet;
import io.global.chat.chatroom.ChatMultiOperation;
import io.global.chat.chatroom.RoomInitializerServlet;
import io.global.chat.friendlist.FriendListOperation;
import io.global.chat.roomlist.RoomListOperation;
import io.global.common.SimKey;
import io.global.ot.client.OTDriver;
import io.global.ot.server.GlobalOTNodeImpl;

import static io.datakernel.config.ConfigConverters.ofDuration;
import static io.datakernel.config.ConfigConverters.ofInetSocketAddress;
import static io.datakernel.dns.RemoteAsyncDnsClient.DEFAULT_TIMEOUT;
import static io.datakernel.dns.RemoteAsyncDnsClient.GOOGLE_PUBLIC_DNS;
import static io.datakernel.launchers.initializers.ConfigConverters.ofDnsCache;
import static io.datakernel.launchers.initializers.Initializers.ofEventloop;
import static io.datakernel.launchers.initializers.Initializers.ofHttpServer;
import static io.global.launchers.GlobalConfigConverters.ofSimKey;

public final class ChatModule extends AbstractModule {
	private static final SimKey DEMO_SIM_KEY = SimKey.of(new byte[]{2, 51, -116, -111, 107, 2, -50, -11, -16, -66, -38, 127, 63, -109, -90, -51});

	@Provides
	@Singleton
	Eventloop provideEventloop(Config config) {
		return Eventloop.create()
				.initialize(ofEventloop(config));
	}

	@Provides
	@Singleton
	AsyncHttpServer provideServer(Eventloop eventloop, MiddlewareServlet servlet, Config config) {
		return AsyncHttpServer.create(eventloop, servlet)
				.initialize(ofHttpServer(config.getChild("http")));
	}

	@Provides
	@Singleton
	MiddlewareServlet provideMainServlet(
			RoomInitializerServlet roomInitializerServlet,
			DynamicOTNodeServlet<FriendListOperation> friendsListServlet,
			DynamicOTNodeServlet<RoomListOperation> roomListServlet,
			DynamicOTNodeServlet<ChatMultiOperation> roomServlet
	) {
		return MiddlewareServlet.create()
				.with("/friendList/:privKey", friendsListServlet)
				.with("/roomList/:privKey", roomListServlet)
				.with("/room/:suffix/:privKey", roomServlet)
				.with("/createRoom/:privKey", roomInitializerServlet);
	}

	@Provides
	@Singleton
	IAsyncHttpClient provideClient(Eventloop eventloop, AsyncDnsClient dnsClient) {
		return AsyncHttpClient.create(eventloop)
				.withDnsClient(dnsClient);
	}

	@Provides
	@Singleton
	AsyncDnsClient provideDnsClient(Eventloop eventloop, Config config) {
		RemoteAsyncDnsClient remoteDnsClient = RemoteAsyncDnsClient.create(eventloop)
				.withDnsServerAddress(config.get(ofInetSocketAddress(), "dns.serverAddress", GOOGLE_PUBLIC_DNS))
				.withTimeout(config.get(ofDuration(), "dns.timeout", DEFAULT_TIMEOUT));
		return CachedAsyncDnsClient.create(eventloop, remoteDnsClient, config.get(ofDnsCache(eventloop), "dns.cache", DnsCache.create(eventloop)));
	}

	@Provides
	@Singleton
	OTDriver provideDriver(Eventloop eventloop, GlobalOTNodeImpl node, Config config) {
		SimKey simKey = config.get(ofSimKey(), "credentials.simKey", DEMO_SIM_KEY);
		return new OTDriver(node, simKey);
	}
}
