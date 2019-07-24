package io.datakernel.tag;

public interface TagReplacer {
	void replace(StringBuilder text) throws ReplaceException;
}
