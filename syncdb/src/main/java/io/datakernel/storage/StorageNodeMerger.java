package io.datakernel.storage;

import com.google.common.collect.Ordering;
import io.datakernel.async.AsyncCallable;
import io.datakernel.async.AsyncCallables;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.storage.streams.StreamKeyFilter;
import io.datakernel.stream.StreamConsumer;
import io.datakernel.stream.StreamProducer;
import io.datakernel.stream.processor.StreamReducers.Reducer;
import io.datakernel.stream.processor.StreamSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.datakernel.storage.StreamMergeUtils.mergeStreams;

public class StorageNodeMerger<K extends Comparable<K>, V> implements StorageNode<K, V> {

	private final Eventloop eventloop;
	private final Ordering<K> ordering = Ordering.natural();

	private final Reducer<K, KeyValue<K, V>, KeyValue<K, V>, ?> reducer;
	private final List<? extends StorageNode<K, V>> peers;
	private final PredicateFactory<K, V> predicates;

	public StorageNodeMerger(Eventloop eventloop, Reducer<K, KeyValue<K, V>, KeyValue<K, V>, ?> reducer,
	                         PredicateFactory<K, V> predicates, List<? extends StorageNode<K, V>> peers) {
		this.eventloop = eventloop;
		this.reducer = reducer;
		this.predicates = predicates;
		this.peers = peers;
	}

	@Override
	public CompletionStage<StreamProducer<KeyValue<K, V>>> getSortedOutput(java.util.function.Predicate<K> predicate) {
		assert eventloop.inEventloopThread();
		return mergeStreams(eventloop, ordering, reducer, peers, predicate);
	}

	// TODO: add tests and predicates from each peer, here???

	@Override
	public CompletionStage<StreamConsumer<KeyValue<K, V>>> getSortedInput() {
		final List<AsyncCallable<StreamConsumer<KeyValue<K, V>>>> asyncCallables = createAsyncConsumers();
		return AsyncCallables.callAll(eventloop, asyncCallables).call().thenApply(consumers -> {
			final StreamSplitter<KeyValue<K, V>> splitter = StreamSplitter.create(eventloop);
			for (StreamConsumer<KeyValue<K, V>> consumer : consumers) {
				splitter.newOutput().streamTo(consumer);
			}
			return splitter.getInput();
		});
	}

	private List<AsyncCallable<StreamConsumer<KeyValue<K, V>>>> createAsyncConsumers() {
		final List<AsyncCallable<StreamConsumer<KeyValue<K, V>>>> asyncCallables = new ArrayList<>();
		for (final StorageNode<K, V> peer : peers) {
			asyncCallables.add(() -> peer.getSortedInput().thenCompose(consumer -> predicates.create(peer).thenApply(predicate -> {
				final StreamKeyFilter<K, KeyValue<K, V>> filter = new StreamKeyFilter<>(eventloop, predicate, KeyValue::getKey);
				filter.getOutput().streamTo(consumer);
				return filter.getInput();
			})));
		}
		return asyncCallables;
	}
}
