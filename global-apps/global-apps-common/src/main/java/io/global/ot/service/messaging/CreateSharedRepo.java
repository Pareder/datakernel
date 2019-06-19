package io.global.ot.service.messaging;

import io.global.common.PubKey;

import java.util.Set;

public final class CreateSharedRepo {
	private final String id;
	private final Set<PubKey> participants;

	public CreateSharedRepo(String id, Set<PubKey> participants) {
		this.id = id;
		this.participants = participants;
	}

	public String getId() {
		return id;
	}

	public Set<PubKey> getParticipants() {
		return participants;
	}
}
