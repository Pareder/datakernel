package io.datakernel.storage;

import com.google.common.base.Predicate;
import io.datakernel.async.ResultCallback;
import io.datakernel.stream.StreamConsumer;
import io.datakernel.stream.StreamProducer;

// K extends Comparable<K> ???
public interface StorageNode<K, V> {

	// implements Map.Entry<K, V> ???
	class KeyValue<K, V> {
		private final K key;
		private final V value;

		public KeyValue(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			KeyValue<?, ?> keyValue = (KeyValue<?, ?>) o;

			if (key != null ? !key.equals(keyValue.key) : keyValue.key != null) return false;
			return value != null ? value.equals(keyValue.value) : keyValue.value == null;

		}

		@Override
		public int hashCode() {
			int result = key != null ? key.hashCode() : 0;
			result = 31 * result + (value != null ? value.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return key + ": " + value;
		}
	}

	void getSortedOutput(Predicate<K> predicate, ResultCallback<StreamProducer<KeyValue<K, V>>> callback);

	void getSortedInput(ResultCallback<StreamConsumer<KeyValue<K, V>>> callback);

}
