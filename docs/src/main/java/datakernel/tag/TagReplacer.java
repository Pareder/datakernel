package datakernel.tag;

import io.datakernel.async.Promise;

public interface TagReplacer {
	Promise<String> replace(String text);
}
