package io.datakernel.dao;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface ResourceDao {
	String getResource(String resourceName) throws IOException;
}
