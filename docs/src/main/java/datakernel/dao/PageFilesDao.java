package datakernel.dao;

import datakernel.model.PageView;
import io.datakernel.async.Promise;
import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.bytebuf.ByteBufPool;
import io.datakernel.bytebuf.ByteBufQueue;
import io.datakernel.config.Config;
import io.datakernel.csp.file.ChannelFileReader;
import io.datakernel.di.annotation.Inject;
import io.datakernel.di.annotation.Named;
import io.datakernel.util.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.datakernel.config.ConfigConverters.ofPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.regex.Pattern.DOTALL;

public final class PageFilesDao implements PageDao {
	private static final String DEFAULT_TITLE = "unknown";
	private static final String TITLE_REGEX = "title:\\s*(.+)";
	private static final String DESTINATION_REGEX = "Destination:\\s*(.+)";
	private static final String PROPERTIES_BLOCK_REGEX = "---(.*)---";
	private static final String PROPERTIES_REGEX = "(\\.+):\\s*(.+)";
	private final int PROPERTIES_BLOCK_SIZE_LIMIT = ApplicationSettings.getInt(
			PageFilesDao.class, "propertiesBlockSize.limit", 1 << 8);

	private final Pattern propertiesBlockPattern = Pattern.compile(PROPERTIES_BLOCK_REGEX, DOTALL);
	private final Pattern propertiesPattern = Pattern.compile(PROPERTIES_REGEX, DOTALL);
	private final Pattern titlePattern = Pattern.compile(TITLE_REGEX);
	private final Pattern destinationPattern = Pattern.compile(DESTINATION_REGEX);

	private static final String EMPTY = "";
	private static final String DOT = ".";

	private final Path path;
	private final String extension;
	private final Executor executor;

	@Inject
	public PageFilesDao(@Named("files") Config config, Executor executor) {
		this.executor = executor;
		this.extension = config.get("sourceFile.extension");
		this.path = config.get(ofPath(), "sourceFile.path");
	}

	@Override
	public Promise<PageView> loadPage(@NotNull String sector, @Nullable String destination, @NotNull String doc) {
		Path path = this.path.resolve(resolveResource(sector, destination, doc));
		return ChannelFileReader.open(executor, path)
				.then(fileReader -> fileReader.toCollector(ByteBufQueue.collector()))
				.then(buf -> Promise.ofBlockingCallable(executor, () -> {
					String content = buf.asString(defaultCharset());
					PageView pageView = new PageView(sector, content, parseProperties(content));
					Files.walkFileTree(this.path.resolve(sector), new PageVisitor(sector, pageView));
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

	private String resolveResource(@NotNull String sector, @Nullable String destination, @NotNull String doc) {
		String resource = sector + "/" + (destination != null ? destination : "");
		resource = !resource.isEmpty() ?
				resource + "/" + doc + DOT + extension :
				doc + "." + extension;
		return resource;
	}

	private final class PageVisitor extends SimpleFileVisitor<Path> {
		private final String sector;
		private final PageView pageView;

		public PageVisitor(String sector, PageView pageView) {
			this.sector = sector;
			this.pageView = pageView;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
			String destinationName = path.getParent().getFileName().toString();
			if (!sector.equals(destinationName)) {
				String docPath = path.getFileName().toString().replace(DOT + extension, EMPTY);
				ByteBuf buf = ByteBufPool.allocate(PROPERTIES_BLOCK_SIZE_LIMIT);
				buf.tail(PROPERTIES_BLOCK_SIZE_LIMIT);
				try {
					FileChannel channel = FileChannel.open(path, READ);
					channel.read(buf.toReadByteBuffer());
				} catch (IOException e) {
					return TERMINATE;
				}
				String propsBlock = buf.asString(Charset.defaultCharset());
				pageView.putSubParagraph(
						findDocTitle(propsBlock), docPath,
						findDestinationTitle(propsBlock), destinationName);
			}
			return CONTINUE;
		}

		private String findDocTitle(String propsBlock) {
			Matcher matcher = titlePattern.matcher(propsBlock);
			return matcher.find() ? matcher.group(1) : DEFAULT_TITLE;
		}

		private String findDestinationTitle(String propsBlock) {
			Matcher matcher = destinationPattern.matcher(propsBlock);
			return matcher.find() ? matcher.group(1) : DEFAULT_TITLE;
		}
	}
}
