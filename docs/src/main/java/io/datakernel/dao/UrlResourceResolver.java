package io.datakernel.dao;

import io.datakernel.config.Config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.datakernel.config.ConfigConverters.ofPath;
import static java.lang.Integer.MAX_VALUE;

public class UrlResourceResolver implements ResourceResolver<String, Path> {
	private static final String URL_PATTERN = "/?(?<fullPath>(/?(?<filename>[\\w-]+)(\\.\\w+)?)*)/?";
	private static final String EXTENSION_PATTERN = "\\.\\w+";
	private static final String INDEX = "index";
	private static final String SLASH = "/";
	private static final String EMPTY = "";
	private static final String DOT = ".";

	private final Pattern extensionPattern = Pattern.compile(EXTENSION_PATTERN);
	private final Pattern urlPattern = Pattern.compile(URL_PATTERN);
	private final String extension;
	private final Path path;

	private UrlResourceResolver(String extension, Path path) {
		this.extension = extension;
		this.path = path;
	}

	public static UrlResourceResolver create(Config config) {
		return new UrlResourceResolver(config.get("sourceFile.extension"), config.get(ofPath(), "sourceFile.path"));
	}

	@Override
	public Path resolve(String resource) throws IOException {
		Matcher matcher = urlPattern.matcher(resource);
		if (matcher.find()) {
			String fullPath = extensionPattern.matcher(matcher.group("fullPath")).replaceAll(EMPTY);
			Path fullResolvedPath = this.path.resolve(fullPath + DOT + extension);
			if (Files.exists(fullResolvedPath)) {
				return fullResolvedPath;
			}
			Path fullResolvedIndexPath = this.path.resolve(fullPath + (!fullPath.isEmpty() ? SLASH : "") + INDEX + DOT + extension);
			if (Files.exists(fullResolvedIndexPath)) {
				return fullResolvedIndexPath;
			}
			String filename = matcher.group("filename");
			return Files.find(this.path, MAX_VALUE,
					(path, attributes) -> attributes.isRegularFile() && path.getFileName().toString().contains(filename))
					.findFirst()
					.orElseThrow(() -> new FileNotFoundException("Cannot map " + resource));
		}
		throw new FileNotFoundException("Cannot map " + resource);
	}
}
