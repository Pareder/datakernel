package io.global.ot.shared;

import io.datakernel.ot.OTState;
import io.global.common.PubKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class SharedReposOTState implements OTState<SharedReposOperation> {
	public static final Consumer<SharedReposOperation> NO_ACTION = op -> {};

	private final Map<String, Set<PubKey>> sharedRepos = new HashMap<>();
	private Consumer<SharedReposOperation> listener = NO_ACTION;

	@Override
	public void init() {
		sharedRepos.clear();
	}

	@Override
	public void apply(SharedReposOperation op) {
		op.getSharedRepos()
				.forEach((key, value) -> {
					if (value.isRemove()) {
						sharedRepos.remove(key);
					} else {
						sharedRepos.put(key, value.getParticipants());
					}
				});
		listener.accept(op);
	}

	public Map<String, Set<PubKey>> getSharedRepos() {
		return sharedRepos;
	}

	public void setListener(Consumer<SharedReposOperation> listener) {
		this.listener = listener;
	}
}
