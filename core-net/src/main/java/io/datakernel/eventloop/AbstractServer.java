/*
 * Copyright (C) 2015 SoftIndex LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datakernel.eventloop;

import io.datakernel.async.MaterializedPromise;
import io.datakernel.async.Promise;
import io.datakernel.async.SettableCallback;
import io.datakernel.eventloop.AsyncTcpSocketImpl.Inspector;
import io.datakernel.inspector.BaseInspector;
import io.datakernel.jmx.EventStats;
import io.datakernel.jmx.EventloopJmxMBeanEx;
import io.datakernel.jmx.JmxAttribute;
import io.datakernel.net.ServerSocketSettings;
import io.datakernel.net.SocketSettings;
import io.datakernel.util.Initializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static io.datakernel.eventloop.AsyncSslSocket.wrapServerSocket;
import static io.datakernel.eventloop.AsyncTcpSocketImpl.wrapChannel;
import static io.datakernel.net.ServerSocketSettings.DEFAULT_BACKLOG;
import static io.datakernel.util.Preconditions.check;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * It is implementation of {@link EventloopServer}. It is non-blocking server which works in eventloop.
 * The server runs on the one thread, and all events are fired on that thread. This server can listen few
 * addresses in one time and you can register multiple connections for responding to incoming data.
 *
 * @param <S> type of AbstractNioServer which extends from it
 */
@SuppressWarnings("WeakerAccess, unused")
public abstract class AbstractServer<S extends AbstractServer<S>> implements EventloopServer, WorkerServer, Initializable<S>, EventloopJmxMBeanEx {
	protected Logger logger = getLogger(getClass());

	@NotNull
	protected final Eventloop eventloop;

	public static final ServerSocketSettings DEFAULT_SERVER_SOCKET_SETTINGS = ServerSocketSettings.create(DEFAULT_BACKLOG);
	public static final SocketSettings DEFAULT_SOCKET_SETTINGS = SocketSettings.create();

	protected ServerSocketSettings serverSocketSettings = DEFAULT_SERVER_SOCKET_SETTINGS;
	protected SocketSettings socketSettings = DEFAULT_SOCKET_SETTINGS;

	protected boolean acceptOnce;

	public interface AcceptFilter {
		boolean filterAccept(SocketChannel socketChannel, InetSocketAddress localAddress, InetAddress remoteAddress, boolean ssl);
	}

	private AcceptFilter acceptFilter;

	protected List<InetSocketAddress> listenAddresses = new ArrayList<>();

	// ssl
	private SSLContext sslContext;
	private Executor sslExecutor;
	protected List<InetSocketAddress> sslListenAddresses = new ArrayList<>();

	private boolean running = false;
	private List<ServerSocketChannel> serverSocketChannels;

	// jmx
	private static final Duration SMOOTHING_WINDOW = Duration.ofMinutes(1);
	AbstractServer<?> acceptServer = this;
	@Nullable
	private AsyncTcpSocketImpl.Inspector socketInspector;
	@Nullable
	private AsyncTcpSocketImpl.Inspector socketSslInspector;
	private final EventStats accepts = EventStats.create(SMOOTHING_WINDOW);
	private final EventStats acceptsSsl = EventStats.create(SMOOTHING_WINDOW);
	private final EventStats filteredAccepts = EventStats.create(SMOOTHING_WINDOW);

	// region creators & builder methods
	protected AbstractServer(@NotNull Eventloop eventloop) {
		this.eventloop = eventloop;
	}

	@SuppressWarnings("unchecked")
	public final S withAcceptFilter(AcceptFilter acceptFilter) {
		this.acceptFilter = acceptFilter;
		return (S) this;
	}

	@SuppressWarnings("unchecked")
	public final S withServerSocketSettings(ServerSocketSettings serverSocketSettings) {
		this.serverSocketSettings = serverSocketSettings;
		return (S) this;
	}

	@SuppressWarnings("unchecked")
	public final S withSocketSettings(SocketSettings socketSettings) {
		this.socketSettings = socketSettings;
		return (S) this;
	}

	@SuppressWarnings("unchecked")
	public final S withListenAddresses(List<InetSocketAddress> addresses) {
		this.listenAddresses = addresses;
		return (S) this;
	}

	public final S withListenAddresses(InetSocketAddress... addresses) {
		return withListenAddresses(asList(addresses));
	}

	public final S withListenAddress(InetSocketAddress address) {
		return withListenAddresses(singletonList(address));
	}

	public final S withListenPort(int port) {
		return withListenAddress(new InetSocketAddress(port));
	}

	@SuppressWarnings("unchecked")
	public final S withSslListenAddresses(SSLContext sslContext, Executor sslExecutor, List<InetSocketAddress> addresses) {
		this.sslContext = sslContext;
		this.sslExecutor = sslExecutor;
		this.sslListenAddresses = addresses;
		return (S) this;
	}

	public final S withSslListenAddresses(SSLContext sslContext, Executor sslExecutor, InetSocketAddress... addresses) {
		return withSslListenAddresses(sslContext, sslExecutor, asList(addresses));
	}

	public final S withSslListenAddress(SSLContext sslContext, Executor sslExecutor, InetSocketAddress address) {
		return withSslListenAddresses(sslContext, sslExecutor, singletonList(address));
	}

	public final S withSslListenPort(SSLContext sslContext, Executor sslExecutor, int port) {
		return withSslListenAddress(sslContext, sslExecutor, new InetSocketAddress(port));
	}

	public final S withAcceptOnce() {
		return withAcceptOnce(true);
	}

	@SuppressWarnings("unchecked")
	public final S withAcceptOnce(boolean acceptOnce) {
		this.acceptOnce = acceptOnce;
		return (S) this;
	}

	@SuppressWarnings("unchecked")
	public final S withSocketInspector(Inspector socketInspector) {
		this.socketInspector = socketInspector;
		return (S) this;
	}

	@SuppressWarnings("unchecked")
	public final S withSocketSslInspector(Inspector socketSslInspector) {
		this.socketSslInspector = socketSslInspector;
		return (S) this;
	}

	@SuppressWarnings("unchecked")
	public final S withLogger(Logger logger) {
		this.logger = logger;
		return (S) this;
	}
	// endregion

	protected abstract void serve(AsyncTcpSocket socket, InetAddress remoteAddress);

	protected void onListen() {
	}

	protected void onClose(SettableCallback<Void> cb) {
		cb.set(null);
	}

	protected void onAccept(SocketChannel socketChannel, InetSocketAddress localAddress, InetAddress remoteAddress, boolean ssl) {
	}

	protected void onFilteredAccept(SocketChannel socketChannel, InetSocketAddress localAddress, InetAddress remoteAddress, boolean ssl) {
	}

	@Override
	public final void listen() throws IOException {
		check(eventloop.inEventloopThread(), "Not in eventloop thread");
		if (running)
			return;
		running = true;
		onListen();
		serverSocketChannels = new ArrayList<>();
		if (listenAddresses != null && !listenAddresses.isEmpty()) {
			listenAddresses(listenAddresses, false);
			logger.info("Listening on {}: {}", listenAddresses, this);
		}
		if (sslListenAddresses != null && !sslListenAddresses.isEmpty()) {
			listenAddresses(sslListenAddresses, true);
			logger.info("Listening with SSL on {}: {}", sslListenAddresses, this);
		}
	}

	private void listenAddresses(List<InetSocketAddress> addresses, boolean ssl) throws IOException {
		for (InetSocketAddress address : addresses) {
			try {
				ServerSocketChannel serverSocketChannel = eventloop.listen(address, serverSocketSettings,
						channel -> doAccept(channel, address, ssl));
				serverSocketChannels.add(serverSocketChannel);
			} catch (IOException e) {
				logger.error("Can't listen on [" + address + "]: " + this, e);
				close();
				throw e;
			}
		}
	}

	@Override
	public final MaterializedPromise<?> close() {
		check(eventloop.inEventloopThread(), "Cannot close server from different thread");
		if (!running) return Promise.complete();
		running = false;
		closeServerSocketChannels();
		return Promise.ofCallback(this::onClose)
				.whenComplete(($, e) -> {
					if (e == null) {
						logger.info("Server closed: {}", this);
					} else {
						logger.error("Server closed exceptionally: " + this, e);
					}
				})
				.materialize();
	}

	public final Future<?> closeFuture() {
		return eventloop.submit(this::close);
	}

	public final boolean isRunning() {
		return running;
	}

	private void closeServerSocketChannels() {
		if (serverSocketChannels == null || serverSocketChannels.isEmpty()) {
			return;
		}
		for (Iterator<ServerSocketChannel> it = serverSocketChannels.iterator(); it.hasNext(); ) {
			ServerSocketChannel serverSocketChannel = it.next();
			if (serverSocketChannel == null) {
				continue;
			}
			eventloop.closeChannel(serverSocketChannel, serverSocketChannel.keyFor(eventloop.getSelector()));
			it.remove();
		}
	}

	protected WorkerServer getWorkerServer() {
		return this;
	}

	protected Inspector getSocketInspector(InetAddress remoteAddress, InetSocketAddress localAddress, boolean ssl) {
		return ssl ? socketSslInspector : socketInspector;
	}

	private void doAccept(SocketChannel channel, InetSocketAddress localAddress, boolean ssl) {
		assert eventloop.inEventloopThread();

		InetAddress remoteAddress;
		try {
			remoteAddress = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
		} catch (IOException e) {
			eventloop.closeChannel(channel, null);
			return;
		}

		if (acceptFilter != null && acceptFilter.filterAccept(channel, localAddress, remoteAddress, ssl)) {
			filteredAccepts.recordEvent();
			onFilteredAccept(channel, localAddress, remoteAddress, ssl);
			return;
		}

		WorkerServer workerServer = getWorkerServer();
		Eventloop workerServerEventloop = workerServer.getEventloop();

		if (workerServerEventloop == eventloop) {
			workerServer.doAccept(channel, localAddress, remoteAddress, ssl, socketSettings);
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("received connection from [{}]{}: {}", remoteAddress, ssl ? " over SSL" : "", this);
			}
			accepts.recordEvent();
			if (ssl) acceptsSsl.recordEvent();
			onAccept(channel, localAddress, remoteAddress, ssl);
			workerServerEventloop.execute(() ->
					workerServer.doAccept(channel, localAddress, remoteAddress, ssl, socketSettings));
		}

		if (acceptOnce) {
			close();
		}
	}

	@Override
	public final void doAccept(SocketChannel socketChannel, InetSocketAddress localAddress, InetAddress remoteAddress,
			boolean ssl, SocketSettings socketSettings) {
		assert eventloop.inEventloopThread();
		accepts.recordEvent();
		if (ssl) acceptsSsl.recordEvent();
		onAccept(socketChannel, localAddress, remoteAddress, ssl);
		AsyncTcpSocketImpl asyncTcpSocketImpl = wrapChannel(eventloop, socketChannel, socketSettings);
		AsyncTcpSocket asyncTcpSocket = ssl ? wrapServerSocket(asyncTcpSocketImpl, sslContext, sslExecutor) : asyncTcpSocketImpl;
		serve(asyncTcpSocket, remoteAddress);
	}

	private boolean isInetAddressAny(InetSocketAddress listenAddress) {
		return listenAddress.getAddress().isAnyLocalAddress();
	}

	public ServerSocketSettings getServerSocketSettings() {
		return serverSocketSettings;
	}

	public List<InetSocketAddress> getListenAddresses() {
		return listenAddresses;
	}

	public List<InetSocketAddress> getSslListenAddresses() {
		return sslListenAddresses;
	}

	public SocketSettings getSocketSettings() {
		return socketSettings;
	}

	@NotNull
	@Override
	public final Eventloop getEventloop() {
		return eventloop;
	}

	@JmxAttribute(extraSubAttributes = "totalCount")
	@Nullable
	public final EventStats getAccepts() {
		return acceptServer.listenAddresses.isEmpty() ? null : accepts;
	}

	@JmxAttribute
	@Nullable
	public final EventStats getAcceptsSsl() {
		return acceptServer.sslListenAddresses.isEmpty() ? null : acceptsSsl;
	}

	@JmxAttribute
	@Nullable
	public final EventStats getFilteredAccepts() {
		return acceptFilter == null ? null : filteredAccepts;
	}

	@JmxAttribute
	@Nullable
	public final AsyncTcpSocketImpl.JmxInspector getSocketStats() {
		return this instanceof PrimaryServer || acceptServer.listenAddresses.isEmpty() ? null :
				BaseInspector.lookup(socketInspector, AsyncTcpSocketImpl.JmxInspector.class);
	}

	@JmxAttribute
	@Nullable
	public final AsyncTcpSocketImpl.JmxInspector getSocketStatsSsl() {
		return this instanceof PrimaryServer || acceptServer.sslListenAddresses.isEmpty() ? null :
				BaseInspector.lookup(socketSslInspector, AsyncTcpSocketImpl.JmxInspector.class);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '{' +
				(listenAddresses.isEmpty() ? "" : "listenAddresses=" + listenAddresses) +
				(sslListenAddresses.isEmpty() ? "" : ", sslListenAddresses=" + sslListenAddresses) +
				(acceptOnce ? ", acceptOnce" : "") +
				'}';
	}

}





