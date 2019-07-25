package io.datakernel.automation;

import io.datakernel.config.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.MAX_VALUE;

public class PageLinkChecker implements PageChecker {
	private static final String EXTENSION_PATTERN = "\\.\\w+";
	private static final String LINK_PATTERN = "\\[.+?]\\([\\w-]*?/?([\\w-]+(\\.\\w+)?)\\)";
	private static final String DOT = ".";
	private static final String EMPTY = "";

	private final Pattern extensionPattern = Pattern.compile(EXTENSION_PATTERN);
	private final Pattern linkPattern = Pattern.compile(LINK_PATTERN);
	private final Path pagesRootPath;
	private final String extension;

	private PageLinkChecker(Path pagesRootPath, String extension) {
		this.pagesRootPath = pagesRootPath;
		this.extension = extension;
	}

	public static PageLinkChecker create(Config config) {
		return new PageLinkChecker(Paths.get(config.get("sourceFile.path")), config.get("sourceFile.extension"));
	}

	@Override
	public Map<String, List<Throwable>> check(StringBuilder content, String fileName) {
		Map<String, List<Throwable>> pathToExceptions = new HashMap<>();
		Matcher linkMatch = linkPattern.matcher(content);
		while (linkMatch.find()) {
			Throwable throwable = null;
			try {
				String resourceName = extensionPattern.matcher(linkMatch.group(1))
						.replaceAll(EMPTY)
						.replace("\\.\\w+", EMPTY)
						.concat(DOT)
						.concat(extension);
				Optional<Path> optionalPath = Files.find(pagesRootPath, MAX_VALUE,
						(path, attributes) -> attributes.isRegularFile() && path.getFileName().toString().equals(resourceName))
						.findFirst();
				if (!optionalPath.isPresent()) {
					throwable = new PageException(String.format("Cannot find link: %s", resourceName));
				}
			} catch (IOException e) {
				throwable = e;
			}
			if (throwable == null) continue;
			List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
			exceptions.add(throwable);
			pathToExceptions.putIfAbsent(fileName, exceptions);
		}
		return pathToExceptions;
	}
}
