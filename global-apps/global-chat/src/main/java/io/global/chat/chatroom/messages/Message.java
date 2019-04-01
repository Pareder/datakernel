package io.global.chat.chatroom.messages;

public class Message {
	private final long timestamp;
	private final String author;
	private final String content;

	public Message(long timestamp, String author, String content) {
		this.timestamp = timestamp;
		this.author = author;
		this.content = content;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getAuthor() {
		return author;
	}

	public String getContent() {
		return content;
	}
}
