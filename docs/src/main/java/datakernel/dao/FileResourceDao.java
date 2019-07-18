package datakernel.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileResourceDao implements ResourceDao {
	private final Path rootPath;

	public FileResourceDao(Path rootPath) {
		this.rootPath = rootPath;
	}

	@Override
	public String getResource(String resourceName) throws IOException {
		Path resolvedPath = rootPath.resolve(resourceName);
		return new String(Files.readAllBytes(resolvedPath));
	}
}
