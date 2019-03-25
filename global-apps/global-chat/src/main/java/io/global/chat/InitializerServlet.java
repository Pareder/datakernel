package io.global.chat;

import io.datakernel.async.Promise;
import io.datakernel.async.Promises;
import io.datakernel.codec.StructuredCodec;
import io.datakernel.exception.ParseException;
import io.datakernel.exception.UncheckedException;
import io.datakernel.http.AsyncServlet;
import io.datakernel.http.HttpRequest;
import io.datakernel.http.HttpResponse;
import io.datakernel.ot.OTCommit;
import io.global.chat.friendlist.FriendsListModule;
import io.global.common.PrivKey;
import io.global.ot.api.CommitId;
import io.global.ot.api.RepoID;
import io.global.ot.client.MyRepositoryId;
import io.global.ot.client.OTDriver;
import org.jetbrains.annotations.NotNull;

import static io.global.chat.friendlist.FriendsListOperation.FRIENDS_LIST_CODEC;
import static java.util.Collections.*;

public class InitializerServlet implements AsyncServlet {
	private final OTDriver driver;

	public InitializerServlet(OTDriver driver) {
		this.driver = driver;
	}

	@NotNull
	@Override
	public Promise<HttpResponse> serve(HttpRequest request) throws UncheckedException {
		try {
			PrivKey privKey = PrivKey.fromString(request.getPathParameter("privKey"));
			return Promises.all(initialize(privKey, FriendsListModule.REPOSITORY_NAME, FRIENDS_LIST_CODEC))
					.map($ -> HttpResponse.ok200());
		} catch (ParseException e) {
			return Promise.ofException(e);
		}
	}

	private <D> Promise<Void> initialize(PrivKey privKey, String repositoryName, StructuredCodec<D> diffCodec) {
		RepoID repoID = RepoID.of(privKey.computePubKey(), repositoryName);
		MyRepositoryId<D> myRepositoryId = new MyRepositoryId<>(repoID, privKey, diffCodec);
		return driver.getHeads(repoID)
				.then(heads -> {
					if (!heads.isEmpty()) {
						return Promise.complete();
					}

					OTCommit<CommitId, D> rootCommit = driver.createCommit(0, myRepositoryId, emptyMap(), 1);
					return driver.push(myRepositoryId, rootCommit)
							.then($ -> driver.updateHeads(myRepositoryId, singleton(rootCommit.getId()), emptySet()))
							.then($ -> driver.saveSnapshot(myRepositoryId, rootCommit.getId(), emptyList()));
				});
	}
}
