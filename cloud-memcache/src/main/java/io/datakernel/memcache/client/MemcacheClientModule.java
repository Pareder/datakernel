package io.datakernel.memcache.client;

import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Export;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.AbstractModule;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.memcache.protocol.MemcacheRpcMessage;
import io.datakernel.memcache.protocol.SerializerGenByteBuf;
import io.datakernel.rpc.client.RpcClient;
import io.datakernel.serializer.SerializerBuilder;
import io.datakernel.serializer.asm.SerializerGenBuilderConst;

import java.time.Duration;

import static io.datakernel.config.ConfigConverters.*;
import static io.datakernel.memcache.protocol.MemcacheRpcMessage.HASH_FUNCTION;
import static io.datakernel.rpc.client.RpcClient.DEFAULT_SOCKET_SETTINGS;
import static io.datakernel.rpc.client.sender.RpcStrategies.rendezvousHashing;
import static io.datakernel.util.MemSize.kilobytes;
import static org.slf4j.LoggerFactory.getLogger;

public class MemcacheClientModule extends AbstractModule {

	public static MemcacheClientModule create() { return new MemcacheClientModule(); }

	@Provides
	RpcClient rpcClient(Config config, Eventloop eventloop) {
		return RpcClient.create(eventloop)
				.withStrategy(rendezvousHashing(HASH_FUNCTION)
						.withMinActiveShards(config.get(ofInteger(), "client.minAliveConnections", 1))
						.withShards(config.get(ofList(ofInetSocketAddress()), "client.addresses")))
				.withMessageTypes(MemcacheRpcMessage.MESSAGE_TYPES)
				.withSerializerBuilder(SerializerBuilder.create(ClassLoader.getSystemClassLoader())
						.withSerializer(ByteBuf.class, new SerializerGenBuilderConst(new SerializerGenByteBuf(false))))
				.withStreamProtocol(
						config.get(ofMemSize(), "protocol.packetSize", kilobytes(64)),
						config.get(ofMemSize(), "protocol.packetSizeMax", kilobytes(64)),
						config.get(ofBoolean(), "protocol.compression", false))
				.withSocketSettings(config.get(ofSocketSettings(), "client.socketSettings", DEFAULT_SOCKET_SETTINGS))
				.withConnectTimeout(config.get(ofDuration(), "client.connectSettings.connectTimeout", Duration.ofSeconds(10)))
				.withReconnectInterval(config.get(ofDuration(), "client.connectSettings.reconnectInterval", Duration.ofSeconds(1)))
				.withLogger(getLogger(MemcacheClient.class));
	}

	@Provides
	@Export
	MemcacheClient memcacheClient(RpcClient client, Eventloop eventloop) {
		return new MemcacheClientImpl(client.adaptToAnotherEventloop(eventloop));
	}

}
