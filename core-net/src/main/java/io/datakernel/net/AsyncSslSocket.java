/*
 * Copyright (C) 2015-2018 SoftIndex LLC.
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

package io.datakernel.net;

import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.bytebuf.ByteBufPool;
import io.datakernel.eventloop.net.CloseWithoutNotifyException;
import io.datakernel.promise.Promise;
import io.datakernel.promise.SettablePromise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import static io.datakernel.common.Recyclable.tryRecycle;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;
import static javax.net.ssl.SSLEngineResult.Status.BUFFER_UNDERFLOW;
import static javax.net.ssl.SSLEngineResult.Status.CLOSED;

/**
 * This is an SSL proxy around {@link AsyncTcpSocket}.
 * <p>
 * It allows SSL connections using Java {@link SSLEngine}.
 */
public final class AsyncSslSocket implements AsyncTcpSocket {
	private final SSLEngine engine;
	private final Executor executor;
	private final AsyncTcpSocket upstream;

	private ByteBuf net2engine = ByteBuf.empty();
	private ByteBuf engine2app = ByteBuf.empty();
	private ByteBuf app2engine = ByteBuf.empty();

	@Nullable
	private SettablePromise<ByteBuf> read;
	@Nullable
	private SettablePromise<Void> write;

	public static AsyncSslSocket wrapClientSocket(AsyncTcpSocket asyncTcpSocket,
			String host, int port,
			SSLContext sslContext, Executor executor) {
		SSLEngine sslEngine = sslContext.createSSLEngine(host, port);
		sslEngine.setUseClientMode(true);
		return create(asyncTcpSocket, sslEngine, executor);
	}

	public static AsyncSslSocket wrapClientSocket(AsyncTcpSocket asyncTcpSocket,
			SSLContext sslContext, Executor executor) {
		SSLEngine sslEngine = sslContext.createSSLEngine();
		sslEngine.setUseClientMode(true);
		return create(asyncTcpSocket, sslEngine, executor);
	}

	public static AsyncSslSocket wrapServerSocket(AsyncTcpSocket asyncTcpSocket,
			SSLContext sslContext, Executor executor) {
		SSLEngine sslEngine = sslContext.createSSLEngine();
		sslEngine.setUseClientMode(false);
		return create(asyncTcpSocket, sslEngine, executor);
	}

	private AsyncSslSocket(AsyncTcpSocket asyncTcpSocket, SSLEngine engine, Executor executor) {
		this.engine = engine;
		this.executor = executor;
		this.upstream = asyncTcpSocket;
		startHandShake();
	}

	public static AsyncSslSocket create(AsyncTcpSocket asyncTcpSocket,
			SSLEngine engine, Executor executor) {
		return new AsyncSslSocket(asyncTcpSocket, engine, executor);
	}

	@NotNull
	private <T> Promise<T> sanitize(T value, @Nullable Throwable e) {
		if (e == null) {
			return Promise.of(value);
		} else {
			close(e);
			return Promise.ofException(e);
		}
	}

	@NotNull
	@Override
	public Promise<ByteBuf> read() {
		if (!isOpen()) return Promise.ofException(CLOSE_EXCEPTION);
		read = null;
		if (engine2app.canRead()) {
			ByteBuf readBuf = engine2app;
			engine2app = ByteBuf.empty();
			return Promise.of(readBuf);
		}
		SettablePromise<ByteBuf> read = new SettablePromise<>();
		this.read = read;
		sync();
		return read;
	}

	@NotNull
	@Override
	public Promise<Void> write(@Nullable ByteBuf buf) {
		if (!isOpen()) {
			if (buf != null) {
				buf.recycle();
			}
			return Promise.ofException(CLOSE_EXCEPTION);
		}
		if (buf == null) {
			throw new UnsupportedOperationException("SSL cannot work in half-duplex mode");
		}
		app2engine = ByteBufPool.append(app2engine, buf);
		if (write != null) return write;
		SettablePromise<Void> write = new SettablePromise<>();
		this.write = write;
		sync();
		return write;
	}

	private void doRead() {
		upstream.read()
				.thenEx(this::sanitize)
				.whenResult(buf -> {
					assert isOpen();
					if (buf != null) {
						net2engine = ByteBufPool.append(net2engine, buf);
						sync();
					} else {
						if (engine.isInboundDone()) return;
						try {
							engine.closeInbound();
						} catch (SSLException e) {
							close(new CloseWithoutNotifyException(AsyncSslSocket.class, "Peer closed without sending close_notify", e));
						}
					}
				});
	}

	private void doWrite(ByteBuf dstBuf) {
		upstream.write(dstBuf)
				.thenEx(this::sanitize)
				.whenResult($ -> {
					assert isOpen();
					if (engine.isOutboundDone()) {
						close();
						return;
					}
					if (!app2engine.canRead() && engine.getHandshakeStatus() == NOT_HANDSHAKING && write != null) {
						SettablePromise<Void> write = this.write;
						this.write = null;
						write.set(null);
					}
				});
	}

	private SSLEngineResult tryToUnwrap() throws SSLException {
		ByteBuf dstBuf = ByteBufPool.allocate(engine.getSession().getPacketBufferSize());
		ByteBuffer srcBuffer = net2engine.toReadByteBuffer();
		ByteBuffer dstBuffer = dstBuf.toWriteByteBuffer();

		SSLEngineResult result;
		try {
			result = engine.unwrap(srcBuffer, dstBuffer);
		} catch (SSLException e) {
			dstBuf.recycle();
			throw e;
		} catch (RuntimeException e) {
			// https://bugs.openjdk.java.net/browse/JDK-8072452
			dstBuf.recycle();
			throw new SSLException(e);
		}

		net2engine.ofReadByteBuffer(srcBuffer);
		net2engine = recycleIfEmpty(net2engine);

		dstBuf.ofWriteByteBuffer(dstBuffer);
		if (isOpen() && dstBuf.canRead()) {
			engine2app = ByteBufPool.append(engine2app, dstBuf);
		} else {
			dstBuf.recycle();
		}

		return result;
	}

	private SSLEngineResult tryToWrap() throws SSLException {
		ByteBuf dstBuf = ByteBufPool.allocate(engine.getSession().getPacketBufferSize());
		ByteBuffer srcBuffer = app2engine.toReadByteBuffer();
		ByteBuffer dstBuffer = dstBuf.toWriteByteBuffer();

		SSLEngineResult result;
		try {
			result = engine.wrap(srcBuffer, dstBuffer);
		} catch (SSLException e) {
			dstBuf.recycle();
			throw e;
		} catch (RuntimeException e) {
			// https://bugs.openjdk.java.net/browse/JDK-8072452
			dstBuf.recycle();
			throw new SSLException(e);
		}

		app2engine.ofReadByteBuffer(srcBuffer);
		app2engine = recycleIfEmpty(app2engine);

		dstBuf.ofWriteByteBuffer(dstBuffer);
		if (dstBuf.canRead()) {
			doWrite(dstBuf);
		} else {
			dstBuf.recycle();
		}
		return result;
	}

	/**
	 * This method is used for handling handshake routine as well as sending close_notify message to recipient
	 */
	private void doHandshake() throws SSLException {
		SSLEngineResult result = null;
		while (true) {
			if (result != null && result.getStatus() == CLOSED) {
				close();
				return;
			}

			HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
			if (handshakeStatus == NEED_WRAP) {
				result = tryToWrap();
			} else if (handshakeStatus == NEED_UNWRAP) {
				result = tryToUnwrap();
				if (result.getStatus() == BUFFER_UNDERFLOW) {
					doRead();
					return;
				}
			} else if (handshakeStatus == NEED_TASK) {
				executeTasks();
				return;
			} else {
				doSync();
				return;
			}
		}
	}

	private void executeTasks() {
		while (true) {
			Runnable task = engine.getDelegatedTask();
			if (task == null) break;
			Promise.ofBlockingRunnable(executor, task::run)
					.whenResult($ -> {
						if (!isOpen()) return;
						try {
							doHandshake();
						} catch (SSLException e) {
							close(e);
						}
					});
		}
	}

	private void sync() {
		try {
			doSync();
		} catch (SSLException e) {
			close(e);
		}
	}

	private void doSync() throws SSLException {
		if (!isOpen()) return;
		SSLEngineResult result = null;
		HandshakeStatus handshakeStatus = engine.getHandshakeStatus();

		if (handshakeStatus != NOT_HANDSHAKING) {
			doHandshake();
			return;
		}

		// write data to net
		if (app2engine.canRead()) {
			do {
				result = tryToWrap();
			}
			while (isOpen() && app2engine.canRead() && (result.bytesConsumed() != 0 || result.bytesProduced() != 0));
		}

		if (!isOpen()) {
			return;
		}

		// read data from net
		if (net2engine.canRead()) {
			do {
				result = tryToUnwrap();
			} while (net2engine.canRead() && (result.bytesConsumed() != 0 || result.bytesProduced() != 0));

			if (read != null && engine2app.canRead()) {
				SettablePromise<ByteBuf> read = this.read;
				this.read = null;
				ByteBuf readBuf = engine2app;
				engine2app = ByteBuf.empty();
				read.set(readBuf);
				return;
			}
		}

		if (result != null && result.getStatus() == CLOSED) {
			close();
			return;
		}

		doRead();
	}

	private static ByteBuf recycleIfEmpty(ByteBuf buf) {
		if (buf.canRead())
			return buf;
		buf.recycle();
		return ByteBuf.empty();
	}

	private void startHandShake() {
		try {
			engine.beginHandshake();
			sync();
		} catch (SSLException e) {
			close(e);
		}
	}

	private boolean isOpen() {
		return net2engine != null;
	}

	@SuppressWarnings("AssignmentToNull") // bufs set to null only when socket is closing
	private void recycleByteBufs() {
		tryRecycle(net2engine);
		tryRecycle(engine2app);
		tryRecycle(app2engine);
		net2engine = engine2app = app2engine = null;
	}

	@Override
	public void close(@NotNull Throwable e) {
		if (!isOpen()) return;
		if (!engine.isOutboundDone()) {
			engine.closeOutbound();
			sync(); // sync is used here to send close_notify message to recipient (will be sent once)
		}
		recycleByteBufs();
		upstream.close(e);
		if (write != null) {
			write.setException(e);
			write = null;
		}
		if (read != null) {
			read.setException(e);
			read = null;
		}
	}
}
