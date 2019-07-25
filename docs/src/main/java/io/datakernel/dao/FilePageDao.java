package io.datakernel.dao;

import io.datakernel.async.Promise;
import io.datakernel.bytebuf.ByteBufQueue;
import io.datakernel.csp.file.ChannelFileReader;
import io.datakernel.di.annotation.Inject;
import io.datakernel.model.PageView;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllBytes;
import static java.util.regex.Pattern.DOTALL;

public final class FilePageDao implements PageDao {
	private static final String DESTINATIONS_FILE = "destination.md";
	private static final String PROPERTIES_BLOCK_REGEX = "---(.*)---";
	private static final String PROPERTIES_REGEX = "([\\w-]+):\\s*([\\w-./]+)";
	private static final String DOC_PATTERN = "\\{\\{\\{((?<doc>[\\w-]+)\\s+path\\s*=\"(?<path>[\\w-]+(.[\\w]+)?)\"\\s+title\\s*=\"(?<title>.*?)\")/}}}";
	private static final String DESTINATION_PATTERN = "\\{\\{(?<destination>[\\w-]+)\\s+title=\"(?<title>.*?)\"}}(?<inner>.+)\\{\\{/\\1}}";
	private static final String SECTOR_PATTERN = "\\{(?<sector>[\\w-]+)}(?<inner>.+)\\{/\\1}";
	private static final int MAX_DEPTH = 2;
	private final Pattern propertiesBlockPattern = Pattern.compile(PROPERTIES_BLOCK_REGEX, DOTALL);
	private final Pattern propertiesPattern = Pattern.compile(PROPERTIES_REGEX, DOTALL);
	private final Pattern docPattern = Pattern.compile(DOC_PATTERN, DOTALL);
	private final Pattern destinationPattern = Pattern.compile(DESTINATION_PATTERN, DOTALL);
	private final Pattern sectorPattern = Pattern.compile(SECTOR_PATTERN, DOTALL);

	private final ResourceResolver<String, Path> resourceResolver;
	private final Executor executor;

	@Inject
	public FilePageDao(Executor executor, ResourceResolver<String, Path> resourceResolver) {
		this.executor = executor;
		this.resourceResolver = resourceResolver;
	}

	@Override
	public Promise<PageView> loadPage(@NotNull String resource) {
		try {
			return loadPage(resourceResolver.resolve(resource));
		} catch (IOException e) {
			return Promise.ofException(e);
		}
	}

	@Override
	public Promise<PageView> loadPage(@NotNull Path resource) {
		return ChannelFileReader.open(executor, resource)
				.then(fileReader -> fileReader.toCollector(ByteBufQueue.collector()))
				.then(buf -> Promise.ofBlockingCallable(executor, () -> {
					String content = buf.asString(defaultCharset());
					PageView pageView = new PageView(content, parseProperties(content));
					readDestinationsBar(pageView, resource, MAX_DEPTH);
					return pageView;
				}));
	}

	private Map<String, String> parseProperties(String content) {
		Map<String, String> properties = new HashMap<>();
		Matcher blockMatch = propertiesBlockPattern.matcher(content);
		if (blockMatch.find()) {
			String propsContent = blockMatch.group(1);
			Matcher propsMatch = propertiesPattern.matcher(propsContent);
			while (propsMatch.find()) {
				String key = propsMatch.group(1);
				String value = propsMatch.group(2);
				properties.put(key, value);
			}
		}
		return properties;
	}

	private void readDestinationsBar(PageView pageView, Path resource, int maxDepth) throws IOException {
		if (maxDepth == 0) return;
		Path foundPath = Files.find(resource, MAX_VALUE,
				(path, attributes) -> attributes.isRegularFile() && path.getFileName().toString().equals(DESTINATIONS_FILE))
				.findFirst()
				.orElse(null);

		if (foundPath == null) {
			readDestinationsBar(pageView, resource.getParent(), --maxDepth);
			return;
		}
		String content = new String(readAllBytes(foundPath), defaultCharset());
		Matcher sectorMatch = sectorPattern.matcher(content);
		while (sectorMatch.find()) {
			String innerSector = sectorMatch.group("inner");
			Matcher destinationMatch = destinationPattern.matcher(innerSector);
			while (destinationMatch.find()) {
				String destinationTitle = destinationMatch.group("title");
				String innerDestination = destinationMatch.group("inner");
				Matcher docMatch = docPattern.matcher(innerDestination);
				while (docMatch.find()) {
					String docTitle = docMatch.group("title");
					String docPath = docMatch.group("path");
					pageView.putSubParagraph(docTitle, docPath, destinationTitle);
				}
			}
		}
	}
}
