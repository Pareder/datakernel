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

package io.datakernel.stream;

import io.datakernel.annotation.Nullable;
import io.datakernel.async.MaterializedStage;
import io.datakernel.async.SettableStage;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.exception.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

import static io.datakernel.stream.StreamCapability.LATE_BINDING;
import static io.datakernel.stream.StreamStatus.*;
import static io.datakernel.util.Preconditions.checkNotNull;
import static io.datakernel.util.Preconditions.checkState;
import static java.util.Collections.emptySet;

/**
 * It is basic implementation of {@link StreamConsumer}
 *
 * @param <T> type of received item
 */
public abstract class AbstractStreamConsumer<T> implements StreamConsumer<T> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected final Eventloop eventloop = Eventloop.getCurrentEventloop();
	private final long createTick = eventloop.tick();

	private StreamProducer<T> producer;

	private StreamStatus status = OPEN;
	private Throwable exception;

	private final SettableStage<Void> endOfStream = new SettableStage<>();

	private Object tag;

	/**
	 * Sets wired producer. It will sent data to this consumer
	 *
	 * @param producer stream producer for setting
	 */

	@Override
	public final void setProducer(StreamProducer<T> producer) {
		checkNotNull(producer);
		checkState(this.producer == null);
		checkState(getCapabilities().contains(LATE_BINDING) || eventloop.tick() == createTick,
				LATE_BINDING_ERROR_MESSAGE, this);
		this.producer = producer;
		onWired();
		producer.getEndOfStream()
				.thenRun(this::endOfStream)
				.whenException(this::closeWithError);
	}

	protected void onWired() {
		eventloop.post(this::onStarted);
	}

	protected void onStarted() {
	}

	public boolean isWired() {
		return producer != null;
	}

	public final StreamProducer<T> getProducer() {
		return producer;
	}

	protected final void endOfStream() {
		if (status.isClosed())
			return;
		status = END_OF_STREAM;

		onEndOfStream();
		eventloop.post(this::cleanup);
		endOfStream.set(null);
	}

	protected abstract void onEndOfStream();

	@Override
	public final void closeWithError(Throwable t) {
		if (status.isClosed())
			return;
		status = CLOSED_WITH_ERROR;
		exception = t;
		if (!(t instanceof ExpectedException)) {
			if (logger.isWarnEnabled()) {
				logger.warn("StreamConsumer {} closed with error {}", this, t.toString());
			}
		}
		onError(t);
		eventloop.post(this::cleanup);
		endOfStream.setException(t);
	}

	protected abstract void onError(Throwable t);

	protected void cleanup() {
	}

	public final StreamStatus getStatus() {
		return status;
	}

	public final Throwable getException() {
		return exception;
	}

	@Override
	public final MaterializedStage<Void> getEndOfStream() {
		return endOfStream;
	}

	/** This method is useful for stream transformers that might add some capability to the stream */
	protected static Set<StreamCapability> addCapabilities(@Nullable StreamConsumer<?> consumer,
			StreamCapability capability, StreamCapability... capabilities) {
		EnumSet<StreamCapability> result = EnumSet.of(capability, capabilities);
		if (consumer != null) {
			result.addAll(consumer.getCapabilities());
		}
		return result;
	}

	@Override
	public Set<StreamCapability> getCapabilities() {
		return emptySet();
	}

	public final Object getTag() {
		return tag;
	}

	public final void setTag(Object tag) {
		this.tag = tag;
	}

	@Override
	public String toString() {
		return tag != null ? tag.toString() : super.toString();
	}

}
