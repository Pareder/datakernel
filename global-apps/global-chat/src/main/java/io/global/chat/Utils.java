package io.global.chat;

import io.datakernel.codec.StructuredCodec;
import io.datakernel.exception.ParseException;
import io.datakernel.ot.MergedOTSystem;
import io.datakernel.ot.OTSystem;
import io.global.chat.chatroom.ChatMultiOperation;
import io.global.chat.chatroom.messages.MessagesOTSystem;
import io.global.chat.chatroom.participants.AbstractParticipantsOperation;
import io.global.chat.chatroom.participants.AddParticipants;
import io.global.chat.chatroom.participants.ParticipantsOTSystem;
import io.global.chat.chatroom.participants.RemoveParticipants;
import io.global.chat.chatroom.roomname.RoomNameOTSystem;
import io.global.common.PubKey;
import io.global.ot.api.RepoID;

import java.security.SecureRandom;
import java.util.Base64;

import static io.datakernel.codec.StructuredCodecs.STRING_CODEC;

public final class Utils {
	private Utils() {
		throw new AssertionError();
	}

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	public static final StructuredCodec<PubKey> PUB_KEY_HEX_CODEC = STRING_CODEC.transform(PubKey::fromString, PubKey::asString);
	public static final StructuredCodec<RepoID> REPO_ID_HEX_CODEC = STRING_CODEC.transform(RepoID::fromString, RepoID::asString);
	public static final String ADD_PARTICIPANTS = "Add Participants";
	public static final String REMOVE_PARTICIPANTS = "Remove Participants";

	public static final StructuredCodec<AbstractParticipantsOperation> PARTICIPANTS_CODEC = StructuredCodec.ofObject(
			in -> {
				in.readKey("type");
				String type = in.readString();
				in.readKey("value");
				switch (type) {
					case ADD_PARTICIPANTS:
						return AddParticipants.CODEC.decode(in);
					case REMOVE_PARTICIPANTS:
						return RemoveParticipants.CODEC.decode(in);
					default:
						throw new ParseException("Illegal operation type");
				}
			}, (out, item) -> {
				out.writeKey("type");
				if (item instanceof AddParticipants) {
					out.writeString(ADD_PARTICIPANTS);
					out.writeKey("value", AddParticipants.CODEC, (AddParticipants) item);
				} else if (item instanceof RemoveParticipants) {
					out.writeString(REMOVE_PARTICIPANTS);
					out.writeKey("value", RemoveParticipants.CODEC, (RemoveParticipants) item);
				} else {
					throw new IllegalArgumentException();
				}
			}
	);

	public static String generateRoomName() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().encodeToString(bytes);
	}

	public static OTSystem<ChatMultiOperation> createMergedOTSystem() {
		return MergedOTSystem.mergeOtSystems(ChatMultiOperation::new,
				ChatMultiOperation::getMessageOps, MessagesOTSystem.createOTSystem(),
				ChatMultiOperation::getParticipantsOps, ParticipantsOTSystem.createOTSystem(),
				ChatMultiOperation::getRoomNameOps, RoomNameOTSystem.createOTSystem());
	}
}
