package io.global.common.discovery;

import io.datakernel.async.Promise;
import io.global.common.Hash;
import io.global.common.PubKey;
import io.global.common.SharedSimKey;
import io.global.common.SignedData;
import io.global.common.api.AnnounceData;
import io.global.common.api.DiscoveryService;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static io.global.common.SignedData.UNVERIFIED;

public final class VerifyingDiscoveryService implements DiscoveryService {
	private final DiscoveryService peer;

	public VerifyingDiscoveryService(DiscoveryService peer) {
		this.peer = peer;
	}

	@Override
	public Promise<Void> announce(PubKey space, SignedData<AnnounceData> announceData) {
		if (!announceData.verify(space)) {
			return Promise.ofException(UNVERIFIED);
		}
		return peer.announce(space, announceData);
	}

	@Override
	public Promise<@Nullable SignedData<AnnounceData>> find(PubKey space) {
		return peer.find(space)
				.then(announceData -> {
					if (announceData != null && !announceData.verify(space)) {
						return Promise.ofException(UNVERIFIED);
					}
					return Promise.of(announceData);
				});
	}

	@Override
	public Promise<Void> shareKey(PubKey receiver, SignedData<SharedSimKey> simKey) {
		return peer.shareKey(receiver, simKey);
	}

	@Override
	public Promise<@Nullable SignedData<SharedSimKey>> getSharedKey(PubKey receiver, Hash hash) {
		return peer.getSharedKey(receiver, hash);
	}

	@Override
	public Promise<List<SignedData<SharedSimKey>>> getSharedKeys(PubKey receiver) {
		return peer.getSharedKeys(receiver);
	}
}
