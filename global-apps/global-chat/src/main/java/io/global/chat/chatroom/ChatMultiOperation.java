package io.global.chat.chatroom;

import io.datakernel.codec.StructuredCodec;
import io.global.chat.Utils;
import io.global.chat.chatroom.messages.MessageOperation;
import io.global.chat.chatroom.participants.AbstractParticipantsOperation;
import io.global.chat.chatroom.roomname.ChangeRoomName;

import java.util.ArrayList;
import java.util.List;

import static io.datakernel.codec.StructuredCodecs.object;
import static io.datakernel.codec.StructuredCodecs.ofList;
import static java.util.Arrays.asList;

public final class ChatMultiOperation {
	public static final StructuredCodec<ChatMultiOperation> CODEC = object(ChatMultiOperation::new,
			"messageOps", ChatMultiOperation::getMessageOps, ofList(MessageOperation.CODEC),
			"participantsOps", ChatMultiOperation::getParticipantsOps, ofList(Utils.PARTICIPANTS_CODEC),
			"roomNameOps", ChatMultiOperation::getRoomNameOps, ofList(ChangeRoomName.CODEC));

	private final List<MessageOperation> messageOps;
	private final List<AbstractParticipantsOperation> participantsOps;
	private final List<ChangeRoomName> roomNameOps;

	public ChatMultiOperation(List<MessageOperation> messageOps, List<AbstractParticipantsOperation> participantsOps,
			List<ChangeRoomName> roomNameOps) {
		this.messageOps = messageOps;
		this.participantsOps = participantsOps;
		this.roomNameOps = roomNameOps;
	}

	public static ChatMultiOperation create() {
		return new ChatMultiOperation(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}

	public ChatMultiOperation withMessageOps(MessageOperation... messageOps) {
		this.messageOps.addAll(asList(messageOps));
		return this;
	}

	public ChatMultiOperation withParticipantsOps(AbstractParticipantsOperation... participantsOps) {
		this.participantsOps.addAll(asList(participantsOps));
		return this;
	}

	public ChatMultiOperation withRoomNameOps(ChangeRoomName... roomNameOps) {
		this.roomNameOps.addAll(asList(roomNameOps));
		return this;
	}

	public List<MessageOperation> getMessageOps() {
		return messageOps;
	}

	public List<AbstractParticipantsOperation> getParticipantsOps() {
		return participantsOps;
	}

	public List<ChangeRoomName> getRoomNameOps() {
		return roomNameOps;
	}
}
