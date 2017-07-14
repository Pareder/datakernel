package io.datakernel.storage;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.datakernel.async.CompletionCallbackFuture;
import io.datakernel.async.ResultCallback;
import io.datakernel.async.ResultCallbackFuture;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.eventloop.FatalErrorHandlers;
import io.datakernel.storage.HasSortedStream.KeyValue;
import io.datakernel.stream.StreamConsumers;
import io.datakernel.stream.StreamProducer;
import io.datakernel.stream.processor.StreamReducers;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import static io.datakernel.stream.StreamProducers.ofIterable;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;

public class DataStorageTreeMapTest {
	private static final Predicate<Integer> ALWAYS_TRUE = Predicates.alwaysTrue();

	private Eventloop eventloop;
	private StreamReducers.Reducer<Integer, KeyValue<Integer, Set<String>>, KeyValue<Integer, Set<String>>, Void> reducer;
	private List<? extends HasSortedStream<Integer, Set<String>>> peers;
	private Predicate<Integer> predicate;
	private TreeMap<Integer, Set<String>> state;
	private DataStorageTreeMap<Integer, Set<String>, Void> dataStorageTreeMap;

	private static KeyValue<Integer, Set<String>> newKeyValue(int key, String... value) {
		return new KeyValue<Integer, Set<String>>(key, Sets.newTreeSet(asList(value)));
	}

	@Before
	public void before() {
		eventloop = Eventloop.create().withFatalErrorHandler(FatalErrorHandlers.rethrowOnAnyError());
		reducer = StreamReducers.mergeSortReducer();
		state = new TreeMap<>();
		peers = emptyList();
		predicate = Predicates.alwaysTrue();
		setUpTreeStorage();
	}

	private void setUpTreeStorage() {
		dataStorageTreeMap = new DataStorageTreeMap<>(eventloop, state, peers, reducer, predicate);
	}

	private <T> List<T> toList(StreamProducer<T> producer) {
		final StreamConsumers.ToList<T> streamToList = StreamConsumers.toList(eventloop);
		producer.streamTo(streamToList);
		eventloop.run();
		return streamToList.getList();
	}

	@Test
	public void testInitEmptyState() throws ExecutionException, InterruptedException {
		final ResultCallbackFuture<StreamProducer<KeyValue<Integer, Set<String>>>> callback = ResultCallbackFuture.create();
		dataStorageTreeMap.getSortedStream(ALWAYS_TRUE, callback);

		eventloop.run();
		assertEquals(Collections.emptyList(), toList(callback.get()));
	}

	@Test
	public void testInitNonEmptyState() throws ExecutionException, InterruptedException {
		final List<KeyValue<Integer, Set<String>>> data = asList(newKeyValue(1, "a"), newKeyValue(2, "b", "bb"));
		for (KeyValue<Integer, Set<String>> keyValue : data) state.put(keyValue.getKey(), keyValue.getValue());

		final ResultCallbackFuture<StreamProducer<KeyValue<Integer, Set<String>>>> callback = ResultCallbackFuture.create();
		dataStorageTreeMap.getSortedStream(ALWAYS_TRUE, callback);

		eventloop.run();
		assertEquals(data, toList(callback.get()));
	}

	@Test
	public void testGetSortedStreamPredicate() throws ExecutionException, InterruptedException {
		final List<KeyValue<Integer, Set<String>>> data = asList(newKeyValue(0, "a"), newKeyValue(1, "b"), newKeyValue(2, "c"), newKeyValue(3, "d"));
		for (KeyValue<Integer, Set<String>> keyValue : data) state.put(keyValue.getKey(), keyValue.getValue());

		final ResultCallbackFuture<StreamProducer<KeyValue<Integer, Set<String>>>> callback = ResultCallbackFuture.create();
		dataStorageTreeMap.getSortedStream(Predicates.in(asList(0, 3)), callback);

		eventloop.run();
		assertEquals(asList(data.get(0), data.get(3)), toList(callback.get()));
	}

	@Test
	public void testSynchronize() throws ExecutionException, InterruptedException {
		final KeyValue<Integer, Set<String>> dataId2 = newKeyValue(2, "b");
		state.put(dataId2.getKey(), dataId2.getValue());

		final KeyValue<Integer, Set<String>> dataId1 = newKeyValue(1, "a");
		peers = singletonList(new HasSortedStream<Integer, Set<String>>() {
			@Override
			public void getSortedStream(Predicate<Integer> predicate, ResultCallback<StreamProducer<KeyValue<Integer, Set<String>>>> callback) {
				callback.setResult(ofIterable(eventloop, singleton(dataId1)));
			}
		});
		setUpTreeStorage();

		{
			final ResultCallbackFuture<StreamProducer<KeyValue<Integer, Set<String>>>> callback = ResultCallbackFuture.create();
			dataStorageTreeMap.getSortedStream(ALWAYS_TRUE, callback);

			eventloop.run();
			assertEquals(singletonList(dataId2), toList(callback.get()));
		}
		{
			final CompletionCallbackFuture syncCallback = CompletionCallbackFuture.create();
			dataStorageTreeMap.synchronize(syncCallback);

			eventloop.run();
			syncCallback.get();
		}
		{
			final ResultCallbackFuture<StreamProducer<KeyValue<Integer, Set<String>>>> callback = ResultCallbackFuture.create();
			dataStorageTreeMap.getSortedStream(ALWAYS_TRUE, callback);

			eventloop.run();
			assertEquals(asList(dataId1, dataId2), toList(callback.get()));
		}
	}

	@Test
	public void testAccessors() {
		assertFalse(dataStorageTreeMap.hasKey(1));
		assertNull(dataStorageTreeMap.put(1, ImmutableSet.of("A", "B", "C")));
		assertTrue(dataStorageTreeMap.hasKey(1));
		assertEquals(ImmutableSet.of("A", "B", "C"), dataStorageTreeMap.get(1));
		assertNull(dataStorageTreeMap.get(2));
		assertEquals(1, dataStorageTreeMap.size());
	}
}