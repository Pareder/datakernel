package io.global.chat.chatroom;

import io.datakernel.async.Promise;
import io.datakernel.exception.ParseException;
import io.datakernel.exception.UncheckedException;
import io.datakernel.http.AsyncServlet;
import io.datakernel.http.HttpRequest;
import io.datakernel.http.HttpResponse;
import io.datakernel.ot.OTCommit;
import io.global.chat.chatroom.messages.MessageOperation;
import io.global.common.PrivKey;
import io.global.ot.api.CommitId;
import io.global.ot.api.RepoID;
import io.global.ot.client.MyRepositoryId;
import io.global.ot.client.OTDriver;
import org.jetbrains.annotations.NotNull;

import static io.global.chat.Utils.generateRoomName;
import static io.global.chat.chatroom.messages.MessageOperation.CODEC;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.*;

public final class RoomInitializerServlet implements AsyncServlet {
	private static final String PREFIX = "room/";
	private final OTDriver driver;

	public RoomInitializerServlet(OTDriver driver) {
		this.driver = driver;
	}

	@NotNull
	@Override
	public Promise<HttpResponse> serve(HttpRequest request) throws UncheckedException {
		try {
			PrivKey privKey = PrivKey.fromString(request.getPathParameter("privKey"));
			String roomName = generateRoomName();
			RepoID repoID = RepoID.of(privKey, PREFIX + roomName);
			MyRepositoryId<MessageOperation> myRepositoryId = new MyRepositoryId<>(repoID, privKey, CODEC);
			return driver.getHeads(repoID)
					.then(heads -> {
						if (!heads.isEmpty()) {
							return Promise.complete();
						}

						OTCommit<CommitId, MessageOperation> rootCommit = driver.createCommit(myRepositoryId, emptyMap(), 1);
						return driver.push(myRepositoryId, rootCommit)
								.then($ -> driver.updateHeads(myRepositoryId, singleton(rootCommit.getId()), emptySet()))
								.then($ -> driver.saveSnapshot(myRepositoryId, rootCommit.getId(), emptyList()));
					})
					.map($ -> HttpResponse.ok200()
							.withBody(roomName.getBytes(UTF_8)));
		} catch (ParseException e) {
			return Promise.ofException(e);
		}
	}
}
