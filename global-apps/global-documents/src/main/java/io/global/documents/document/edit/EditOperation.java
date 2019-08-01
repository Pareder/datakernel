package io.global.documents.document.edit;

public interface EditOperation {
	void apply(StringBuilder builder);

	EditOperation invert();

	int getPosition();

	String getContent();

	int getLength();

}
