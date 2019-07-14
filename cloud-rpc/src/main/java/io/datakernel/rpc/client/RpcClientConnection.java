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

package io.datakernel.rpc.client;

import io.datakernel.async.Callback;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.eventloop.ScheduledRunnable;
import io.datakernel.exception.AsyncTimeoutException;
import io.datakernel.jmx.EventStats;
import io.datakernel.jmx.JmxAttribute;
import io.datakernel.jmx.JmxReducers.JmxReducerSum;
import io.datakernel.jmx.JmxRefreshable;
import io.datakernel.rpc.client.jmx.RpcRequestStats;
import io.datakernel.rpc.client.sender.RpcSender;
import io.datakernel.rpc.protocol.*;
import io.datakernel.stream.StreamDataAcceptor;
import io.datakernel.util.Stopwatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import static io.datakernel.rpc.client.IRpcClient.RPC_OVERLOAD_EXCEPTION;
import static io.datakernel.rpc.client.IRpcClient.RPC_TIMEOUT_EXCEPTION;
import static io.datakernel.util.Preconditions.checkArgument;
import static io.datakernel.util.Utils.nullify;
import static org.slf4j.LoggerFactory.getLogger;

public final class RpcClientConnection implements RpcStream.Listener, RpcSender, JmxRefreshable {
	private static final Logger logger = getLogger(RpcClientConnection.class);

	private StreamDataAcceptor<RpcMessage> downstreamDataAcceptor;
	private boolean overloaded = false;

	public static final RpcException CONNECTION_CLOSED = new RpcException(RpcClientConnection.class, "Connection closed.");
	public static final Duration DEFAULT_TIMEOUT_PRECISION = Duration.ofMillis(10);

	private final class TimeoutCookie implements Comparable<TimeoutCookie> {
		private final long timestamp;
		private final int cookie;

		public TimeoutCookie(int cookie, int timeout) {
			this.timestamp = eventloop.currentTimeMillis() + timeout;
			this.cookie = cookie;
		}

		public boolean isExpired() {
			return timestamp < eventloop.currentTimeMillis();
		}

		public int getCookie() {
			return cookie;
		}

		@Override
		public int compareTo(TimeoutCookie o) {
			return Long.compare(timestamp, o.timestamp);
		}
	}

	private final Eventloop eventloop;
	private final RpcClient rpcClient;
	private final RpcStream stream;
	private final InetSocketAddress address;
	private final Map<Integer, Callback<?>> activeRequests = new HashMap<>();
	private final PriorityQueue<TimeoutCookie> timeoutCookies = new PriorityQueue<>();

	private ScheduledRunnable scheduleExpiredResponsesTask;
	private int cookie = 0;
	private Duration timeoutPrecision = DEFAULT_TIMEOUT_PRECISION;
	private boolean serverClosing;

	// JMX
	private boolean monitoring;
	private final RpcRequestStats connectionStats;
	private final EventStats totalRequests;
	private final EventStats connectionRequests;

	RpcClientConnection(Eventloop eventloop, RpcClient rpcClient,
			InetSocketAddress address,
			RpcStream stream) {
		this.eventloop = eventloop;
		this.rpcClient = rpcClient;
		this.stream = stream;
		this.address = address;

		// JMX
		this.monitoring = false;
		this.connectionStats = RpcRequestStats.create(RpcClient.SMOOTHING_WINDOW);
		this.connectionRequests = connectionStats.getTotalRequests();
		this.totalRequests = rpcClient.getGeneralRequestsStats().getTotalRequests();
	}

	public RpcClientConnection withTimeoutPrecision(Duration timeoutPrecision) {
		checkArgument(timeoutPrecision.toMillis() > 0, "Timeout precision cannot be zero or less");
		this.timeoutPrecision = timeoutPrecision;
		return this;
	}

	@Override
	public <I, O> void sendRequest(I request, int timeout, @NotNull Callback<O> cb) {
		assert eventloop.inEventloopThread();

		// jmx
		totalRequests.recordEvent();
		connectionRequests.recordEvent();

		if (!overloaded || request instanceof RpcMandatoryData) {
			cookie++;

			Callback<?> requestCallback = cb;

			// jmx
			if (isMonitoring()) {
				RpcRequestStats requestStatsPerClass = rpcClient.ensureRequestStatsPerClass(((Object) request).getClass());
				requestStatsPerClass.getTotalRequests().recordEvent();
				requestCallback = new JmxConnectionMonitoringResultCallback<>(requestStatsPerClass, (Callback<?>) cb, timeout);
			}

			TimeoutCookie timeoutCookie = new TimeoutCookie(cookie, timeout);
			if (timeoutCookies.isEmpty()) {
				scheduleExpiredResponsesTask();
			}
			timeoutCookies.add(timeoutCookie);
			activeRequests.put(cookie, requestCallback);

			downstreamDataAcceptor.accept(RpcMessage.of(cookie, request));
		} else {
			// jmx
			rpcClient.getGeneralRequestsStats().getRejectedRequests().recordEvent();
			connectionStats.getRejectedRequests().recordEvent();
			if (logger.isTraceEnabled()) logger.trace("RPC client uplink is overloaded");

			cb.accept(null, RPC_OVERLOAD_EXCEPTION);
		}
	}

	private void scheduleExpiredResponsesTask() {
		scheduleExpiredResponsesTask = eventloop.delayBackground(timeoutPrecision, this::checkExpiredResponses);
	}

	private void checkExpiredResponses() {
		scheduleExpiredResponsesTask = null;
		while (!timeoutCookies.isEmpty()) {
			TimeoutCookie timeoutCookie = timeoutCookies.peek();
			if (timeoutCookie == null)
				break;
			if (!timeoutCookie.isExpired())
				break;
			timeoutCookies.remove();
			Callback<?> cb = activeRequests.remove(timeoutCookie.getCookie());
			if (cb != null) {
				// jmx
				connectionStats.getExpiredRequests().recordEvent();
				rpcClient.getGeneralRequestsStats().getExpiredRequests().recordEvent();

				cb.accept(null, RPC_TIMEOUT_EXCEPTION);
			}
		}
		if (serverClosing && activeRequests.size() == 0) {
			shutdown();
		}
		if (!timeoutCookies.isEmpty()) {
			scheduleExpiredResponsesTask();
		}
	}

	@Override
	public void accept(RpcMessage message) {
		if (message.getData().getClass() == RpcRemoteException.class) {
			processErrorMessage(message);
		} else if (message.getData().getClass() == RpcControlMessage.class) {
			processControlMessage((RpcControlMessage) message.getData());
		} else {
			@SuppressWarnings("unchecked")
			Callback<Object> cb = (Callback<Object>) activeRequests.remove(message.getCookie());
			if (cb == null) return;

			cb.accept(message.getData(), null);
			if (serverClosing && activeRequests.size() == 0) {
				shutdown();
			}
		}
	}

	private void processErrorMessage(RpcMessage message) {
		RpcRemoteException remoteException = (RpcRemoteException) message.getData();
		// jmx
		connectionStats.getFailedRequests().recordEvent();
		rpcClient.getGeneralRequestsStats().getFailedRequests().recordEvent();
		connectionStats.getServerExceptions().recordException(remoteException, null);
		rpcClient.getGeneralRequestsStats().getServerExceptions().recordException(remoteException, null);

		Callback<?> cb = activeRequests.remove(message.getCookie());
		if (cb != null) {
			cb.accept(null, remoteException);
		}
	}

	private void processControlMessage(RpcControlMessage controlMessage) {
		if (controlMessage == RpcControlMessage.CLOSE) {
			rpcClient.removeConnection(address);
			serverClosing = true;
			if (activeRequests.size() == 0) {
				shutdown();
			}
		} else {
			throw new RuntimeException("Received unknown RpcControlMessage");
		}
	}

	@Override
	public void onReceiverEndOfStream() {
		logger.info("Receiver EOS: " + address);
		stream.close();
		doClose();
	}

	@Override
	public void onReceiverError(@NotNull Throwable e) {
		logger.error("Receiver error: " + address, e);
		rpcClient.getLastProtocolError().recordException(e, address);
		stream.close();
		doClose();
	}

	@Override
	public void onSenderError(@NotNull Throwable e) {
		logger.error("Sender error: " + address, e);
		rpcClient.getLastProtocolError().recordException(e, address);
		stream.close();
		doClose();
	}

	@Override
	public void onSenderReady(@NotNull StreamDataAcceptor<RpcMessage> acceptor) {
		downstreamDataAcceptor = acceptor;
		overloaded = false;
	}

	@Override
	public void onSenderSuspended() {
		overloaded = true;
	}

	private void doClose() {
		rpcClient.removeConnection(address);

		scheduleExpiredResponsesTask = nullify(scheduleExpiredResponsesTask, ScheduledRunnable::cancel);

		while (!activeRequests.isEmpty()) {
			for (Integer cookie : new HashSet<>(activeRequests.keySet())) {
				Callback<?> cb = activeRequests.remove(cookie);
				cb.accept(null, CONNECTION_CLOSED);
			}
		}
	}

	public void shutdown() {
		stream.sendEndOfStream();
	}

	@Override
	public String toString() {
		return "RpcClientConnection{address=" + address + '}';
	}

	// JMX

	public void startMonitoring() {
		monitoring = true;
	}

	public void stopMonitoring() {
		monitoring = false;
	}

	private boolean isMonitoring() {
		return monitoring;
	}

	@JmxAttribute(name = "")
	public RpcRequestStats getRequestStats() {
		return connectionStats;
	}

	@JmxAttribute(reducer = JmxReducerSum.class)
	public int getActiveRequests() {
		return activeRequests.size();
	}

	@Override
	public void refresh(long timestamp) {
		connectionStats.refresh(timestamp);
	}

	private final class JmxConnectionMonitoringResultCallback<T> implements Callback<T> {
		private final Stopwatch stopwatch;
		private final Callback<T> callback;
		private final RpcRequestStats requestStatsPerClass;
		private final long dueTimestamp;

		public JmxConnectionMonitoringResultCallback(RpcRequestStats requestStatsPerClass, Callback<T> cb,
				long timeout) {
			this.stopwatch = Stopwatch.createStarted();
			this.callback = cb;
			this.requestStatsPerClass = requestStatsPerClass;
			this.dueTimestamp = eventloop.currentTimeMillis() + timeout;
		}

		@Override
		public void accept(T result, @Nullable Throwable e) {
			if (e == null) {
				onResult(result);
			} else {
				onException(e);
			}
		}

		private void onResult(T result) {
			int responseTime = timeElapsed();
			connectionStats.getResponseTime().recordValue(responseTime);
			requestStatsPerClass.getResponseTime().recordValue(responseTime);
			rpcClient.getGeneralRequestsStats().getResponseTime().recordValue(responseTime);
			recordOverdue();
			callback.accept(result, null);
		}

		private void onException(@NotNull Throwable e) {
			if (e instanceof RpcRemoteException) {
				int responseTime = timeElapsed();
				connectionStats.getFailedRequests().recordEvent();
				connectionStats.getResponseTime().recordValue(responseTime);
				connectionStats.getServerExceptions().recordException(e, null);
				requestStatsPerClass.getFailedRequests().recordEvent();
				requestStatsPerClass.getResponseTime().recordValue(responseTime);
				rpcClient.getGeneralRequestsStats().getResponseTime().recordValue(responseTime);
				requestStatsPerClass.getServerExceptions().recordException(e, null);
				recordOverdue();
			} else if (e instanceof AsyncTimeoutException) {
				connectionStats.getExpiredRequests().recordEvent();
				requestStatsPerClass.getExpiredRequests().recordEvent();
			} else if (e instanceof RpcOverloadException) {
				connectionStats.getRejectedRequests().recordEvent();
				requestStatsPerClass.getRejectedRequests().recordEvent();
			}
			callback.accept(null, e);
		}

		private int timeElapsed() {
			return (int) stopwatch.elapsed(TimeUnit.MILLISECONDS);
		}

		private void recordOverdue() {
			int overdue = (int) (System.currentTimeMillis() - dueTimestamp);
			if (overdue > 0) {
				connectionStats.getOverdues().recordValue(overdue);
				requestStatsPerClass.getOverdues().recordValue(overdue);
				rpcClient.getGeneralRequestsStats().getOverdues().recordValue(overdue);
			}
		}
	}
}
