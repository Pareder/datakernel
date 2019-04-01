package io.global.chat.chatroom.participants;

import io.datakernel.codec.StructuredCodec;
import io.global.chat.chatroom.ChatRoomOTState;
import io.global.common.PubKey;

import java.util.Set;

import static io.datakernel.codec.StructuredCodecs.object;
import static io.datakernel.codec.StructuredCodecs.ofSet;
import static io.global.chat.Utils.PUB_KEY_HEX_CODEC;

public final class RemoveParticipants extends AbstractParticipantsOperation {
	public static final StructuredCodec<RemoveParticipants> CODEC = object(RemoveParticipants::new,
			"participants", RemoveParticipants::getParticipants, ofSet(PUB_KEY_HEX_CODEC));

	private RemoveParticipants(Set<PubKey> participants) {
		super(participants);
	}

	public static RemoveParticipants remove(Set<PubKey> participants) {
		return new RemoveParticipants(participants);
	}

	@Override
	public void apply(ChatRoomOTState chatRoomOTState) {
		chatRoomOTState.removeParticipants(participants);
	}
}
