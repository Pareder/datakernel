package io.datakernel.automation;

import io.datakernel.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageLinkChecker implements PageChecker {
	private static final String LINK_PATTERN = "\\[.+?]\\(/(.*?)\\)";
	private static final String LINK_PART_PATTERN = "\\[.+?]\\(/(\\w+?)(/\\w+?)?(/\\w+?)?\\)";
	private final Pattern linkPartPattern = Pattern.compile(LINK_PART_PATTERN);
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
			String link = linkMatch.group();
			Matcher linkPartMatch = linkPartPattern.matcher(link);
			if (linkPartMatch.find()) {
				String sector = linkPartMatch.group(1);
				String destination = linkPartMatch.group(2);
				String doc = linkPartMatch.group(3);
				String path = resolveResource(sector, destination, doc);
				Path pathToPage = pagesRootPath.resolve(path);
				if (!Files.exists(pathToPage)) {
					throwable = new PageException(String.format("Cannot find link: %s", path));
				}
			} else {
				throwable = new PageException(String.format("Incorrect link: %s", link));
			}
			if (throwable == null) continue;
			List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
			exceptions.add(throwable);
			pathToExceptions.putIfAbsent(fileName, exceptions);
		}
		return pathToExceptions;
	}

	protected String resolveResource(@NotNull String sector, @Nullable String destination, @NotNull String doc) {
		String resource = sector + (destination != null ? destination : "");
		resource = !resource.isEmpty() ?
				resource + doc + "." + extension :
				doc + "." + extension;
		return resource;
	}
}
