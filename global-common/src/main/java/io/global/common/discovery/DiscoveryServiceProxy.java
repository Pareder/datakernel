package io.global.common.discovery;

import io.datakernel.async.Promise;
import io.global.common.Hash;
import io.global.common.PubKey;
import io.global.common.SharedSimKey;
import io.global.common.SignedData;
import io.global.common.api.AnnounceData;
import io.global.common.api.DiscoveryService;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DiscoveryServiceProxy implements DiscoveryService {
	private final DiscoveryService global;
	private final DiscoveryService local;

	private final Set<PubKey> whitelist = new HashSet<>();

	public DiscoveryServiceProxy(DiscoveryService global, DiscoveryService local) {
		this.global = global;
		this.local = local;
	}

	public DiscoveryServiceProxy whitelist(PubKey key) {
		this.whitelist.add(key);
		return this;
	}

	public DiscoveryServiceProxy whitelist(Set<PubKey> whitelist) {
		this.whitelist.addAll(whitelist);
		return this;
	}

	@Override
	public Promise<Void> announce(PubKey space, SignedData<AnnounceData> announceData) {
		Promise<Void> first = whitelist.contains(space) ?
				local.announce(space, announceData) :
				Promise.complete();
		return first.then($ -> global.announce(space, announceData));
	}

	@Override
	public Promise<@Nullable SignedData<AnnounceData>> find(PubKey space) {
		return local.find(space)
				.then(announceData ->
						announceData != null ?
								Promise.of(announceData) :
								global.find(space));
	}

	@Override
	public Promise<Void> shareKey(PubKey receiver, SignedData<SharedSimKey> simKey) {
		return global.shareKey(receiver, simKey);
	}

	@Override
	public Promise<@Nullable SignedData<SharedSimKey>> getSharedKey(PubKey receiver, Hash hash) {
		return global.getSharedKey(receiver, hash);
	}

	@Override
	public Promise<List<SignedData<SharedSimKey>>> getSharedKeys(PubKey receiver) {
		return global.getSharedKeys(receiver);
	}
}
