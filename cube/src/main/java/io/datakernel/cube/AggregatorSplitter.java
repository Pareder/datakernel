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

package io.datakernel.cube;

import io.datakernel.aggregation_db.AggregationQuery;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.logfs.LogCommitTransaction;
import io.datakernel.stream.AbstractStreamTransformer_1_N;
import io.datakernel.stream.StreamConsumer;
import io.datakernel.stream.StreamDataReceiver;
import io.datakernel.stream.StreamProducer;

import java.util.List;

/**
 * Represents a logic for input records pre-processing and splitting across multiple cube inputs.
 *
 * @param <T> type of input records
 */
public abstract class AggregatorSplitter<T> extends AbstractStreamTransformer_1_N<T> {
	public interface Factory<T> {
		AggregatorSplitter<T> create(Eventloop eventloop);
	}

	private Cube cube;
	private LogCommitTransaction<?> transaction;

	public AggregatorSplitter(Eventloop eventloop) {
		super(eventloop);
		this.upstreamConsumer = new UpstreamConsumer();
	}

	private final class UpstreamConsumer extends AbstractUpstreamConsumer implements StreamDataReceiver<T> {
		@Override
		protected void onUpstreamEndOfStream() {
			for (AbstractDownstreamProducer<?> downstreamProducer : downstreamProducers) {
				downstreamProducer.sendEndOfStream();
			}
		}

		@Override
		public StreamDataReceiver<T> getDataReceiver() {
			return this;
		}

		@Override
		public void onData(T item) {
			processItem(item);
		}
	}

	private final class DownstreamProducer extends AbstractDownstreamProducer<T> {
		@Override
		protected final void onDownstreamSuspended() {
			upstreamConsumer.getUpstream().onConsumerSuspended();
		}

		@Override
		protected final void onDownstreamResumed() {
			if (allOutputsResumed()) {
				upstreamConsumer.getUpstream().onConsumerResumed();
			}
		}
	}

	public final StreamProducer<T> newOutput() {
		return addOutput(new DownstreamProducer());
	}

	protected final <O> StreamDataReceiver<O> addOutput(Class<O> aggregationItemType, List<String> dimensions, List<String> measures) {
		return addOutput(aggregationItemType, dimensions, measures, null);
	}

	@SuppressWarnings("unchecked")
	protected final <O> StreamDataReceiver<O> addOutput(Class<O> aggregationItemType, List<String> dimensions, List<String> measures,
	                                              AggregationQuery.QueryPredicates predicates) {
		StreamProducer streamProducer = newOutput();
		StreamConsumer streamConsumer = cube.consumer(aggregationItemType, dimensions, measures, predicates,
				transaction.addCommitCallback());
		streamProducer.streamTo(streamConsumer);
		return streamConsumer.getDataReceiver();
	}

	protected abstract void addOutputs();

	protected abstract void processItem(T item);

	public final void streamTo(Cube cube, LogCommitTransaction<?> transaction) {
		this.cube = cube;
		this.transaction = transaction;
		addOutputs();
	}
}
