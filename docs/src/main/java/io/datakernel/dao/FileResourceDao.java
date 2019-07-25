package io.datakernel.dao;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllBytes;

public class FileResourceDao implements ResourceDao {
	private final Path rootPath;

	private FileResourceDao(Path rootPath) {
		this.rootPath = rootPath;
	}

	public static FileResourceDao create(Path path) throws FileNotFoundException {
		if (!Files.isDirectory(path)) {
			throw new FileNotFoundException(path.toString());
		}
		return new FileResourceDao(path);
	}

	@Override
	public String getResource(String resourceName) throws IOException {
		Path foundPath = Files.find(rootPath, MAX_VALUE,
				(path, attributes) -> path.getFileName().toString().equals(resourceName))
				.findFirst()
				.orElseThrow(() -> new FileNotFoundException(resourceName));
		return new String(readAllBytes(foundPath), defaultCharset());
	}
}
