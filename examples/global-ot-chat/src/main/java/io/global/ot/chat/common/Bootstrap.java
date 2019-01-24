package io.global.ot.chat.common;

import io.datakernel.async.Promise;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.eventloop.EventloopService;
import io.datakernel.ot.OTCommit;
import io.global.ot.api.CommitId;
import io.global.ot.client.MyRepositoryId;
import io.global.ot.client.OTDriver;
import org.jetbrains.annotations.NotNull;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public final class Bootstrap<D> implements EventloopService {
	private final Eventloop eventloop;
	private final OTDriver driver;
	private final MyRepositoryId<D> myRepositoryId;

	public Bootstrap(Eventloop eventloop, OTDriver driver, MyRepositoryId<D> myRepositoryId) {
		this.eventloop = eventloop;
		this.driver = driver;
		this.myRepositoryId = myRepositoryId;
	}

	@Override
	public @NotNull Eventloop getEventloop() {
		return eventloop;
	}

	@Override
	public @NotNull Promise<Void> start() {
		return driver.getHeads(myRepositoryId.getRepositoryId())
				.thenCompose(heads -> {
					if (!heads.isEmpty()) return Promise.complete();

					OTCommit<CommitId, D> rootCommit = driver.createCommit(myRepositoryId, emptyMap(), 1);
					return driver.push(myRepositoryId, rootCommit)
							.thenCompose($ -> driver.saveSnapshot(myRepositoryId, rootCommit.getId(), emptyList()));
				});
	}

	@Override
	public @NotNull Promise<Void> stop() {
		return Promise.complete();
	}

	public OTDriver getDriver() {
		return driver;
	}

	public MyRepositoryId<D> getMyRepositoryId() {
		return myRepositoryId;
	}
}
