package io.datakernel.dao;

import java.io.IOException;
import java.nio.file.Path;

public interface ResourceResolver<T, R> {
	R resolve(T resource) throws IOException;
}
