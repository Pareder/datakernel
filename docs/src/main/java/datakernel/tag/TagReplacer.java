package datakernel.tag;

public interface TagReplacer {
	void replace(StringBuilder text) throws ReplaceException;
}
