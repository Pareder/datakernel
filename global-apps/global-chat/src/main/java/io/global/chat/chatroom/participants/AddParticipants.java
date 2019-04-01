package io.global.chat.chatroom.participants;

import io.datakernel.codec.StructuredCodec;
import io.global.chat.chatroom.ChatRoomOTState;
import io.global.common.PubKey;

import java.util.Set;

import static io.datakernel.codec.StructuredCodecs.object;
import static io.datakernel.codec.StructuredCodecs.ofSet;
import static io.global.chat.Utils.PUB_KEY_HEX_CODEC;

public final class AddParticipants extends AbstractParticipantsOperation {
	public static final StructuredCodec<AddParticipants> CODEC = object(AddParticipants::new,
			"participants", AddParticipants::getParticipants, ofSet(PUB_KEY_HEX_CODEC));

	private AddParticipants(Set<PubKey> participants) {
		super(participants);
	}

	public static AddParticipants add(Set<PubKey> participants) {
		return new AddParticipants(participants);
	}

	@Override
	public void apply(ChatRoomOTState chatRoomOTState) {
		chatRoomOTState.addParticipants(participants);
	}
}
