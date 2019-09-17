package io.global.kv.api;

import io.datakernel.async.Promise;
import io.global.common.PubKey;

@FunctionalInterface
public interface StorageFactory {
	Promise<? extends KvStorage> create(PubKey space, String table);
}
