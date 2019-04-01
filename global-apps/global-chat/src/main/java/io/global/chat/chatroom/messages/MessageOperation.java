package io.global.chat.chatroom.messages;

import io.datakernel.codec.StructuredCodec;
import io.global.chat.chatroom.ChatRoomOTState;
import io.global.chat.chatroom.ChatRoomOperation;

import static io.datakernel.codec.StructuredCodecs.*;

public final class MessageOperation implements ChatRoomOperation {
	public static final MessageOperation EMPTY = new MessageOperation(0, "", "", false);
	public static final StructuredCodec<MessageOperation> CODEC = object(MessageOperation::new,
			"timestamp", MessageOperation::getTimestamp, LONG_CODEC,
			"author", MessageOperation::getAuthor, STRING_CODEC,
			"content", MessageOperation::getContent, STRING_CODEC,
			"isDelete", MessageOperation::isTombstone, BOOLEAN_CODEC);

	private final long timestamp;
	private final String author;
	private final String content;
	private final boolean isTombstone;

	private MessageOperation(long timestamp, String author, String content, boolean remove) {
		this.timestamp = timestamp;
		this.author = author;
		this.content = content;
		this.isTombstone = remove;
	}

	public static MessageOperation insert(long timestamp, String author, String content) {
		return new MessageOperation(timestamp, author, content, false);
	}

	public static MessageOperation delete(long timestamp, String author, String content) {
		return new MessageOperation(timestamp, author, content, true);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getContent() {
		return content;
	}

	public String getAuthor() {
		return author;
	}

	public boolean isTombstone() {
		return isTombstone;
	}

	@Override
	public void apply(ChatRoomOTState state) {
		Message message = new Message(timestamp, author, content);
		if (isTombstone) {
			state.removeMessage(message);
		} else {
			state.addMessage(message);
		}
	}

	public boolean isEmpty() {
		return content.isEmpty() || author.isEmpty();
	}

	public MessageOperation invert() {
		return new MessageOperation(timestamp, author, content, !isTombstone);
	}

	public boolean isInversionFor(MessageOperation operation) {
		return timestamp == operation.timestamp &&
				author.equals(operation.author) &&
				content.equals(operation.content) &&
				isTombstone != operation.isTombstone;
	}

	@Override
	public String toString() {
		return '{' + (isTombstone ? "-" : "+") +
				content +
				" [" + author +
				"]}";
	}
}
