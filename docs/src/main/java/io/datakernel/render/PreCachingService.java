package io.datakernel.render;

import io.datakernel.async.MaterializedPromise;
import io.datakernel.async.Promise;
import io.datakernel.config.Config;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.eventloop.EventloopService;
import io.datakernel.util.ref.RefInt;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.datakernel.module.ServletsModule.MAIN_PAGE;
import static java.lang.Integer.MAX_VALUE;
import static java.nio.file.FileVisitResult.CONTINUE;

public final class PreCachingService implements EventloopService {
	private static final String URL_PATTERN = ".*/((.+)/(.+))/?";
	private static final Logger logger = LoggerFactory.getLogger(PreCachingService.class);
	private final Pattern urlPattern = Pattern.compile(URL_PATTERN);
	private final PageRenderer pageRenderer;
	private final Eventloop eventloop;
	private final Path pagesRootPath;

	private PreCachingService(Eventloop eventloop, Path pagesRootPath, PageRenderer pageRenderer) {
		this.pagesRootPath = pagesRootPath;
		this.pageRenderer = pageRenderer;
		this.eventloop = eventloop;
	}

	public static PreCachingService create(Eventloop eventloop, Config config, PageRenderer pageRenderer) {
		return new PreCachingService(
				eventloop, Paths.get(config.get("caching.path")), pageRenderer);
	}

	@Override
	public @NotNull Eventloop getEventloop() {
		return eventloop;
	}

	@Override
	public @NotNull MaterializedPromise<?> start() {
		logger.info("Start caching");
		RefInt count = new RefInt(0);
		try {
			Files.find(pagesRootPath, MAX_VALUE, (path, attributes) -> false);
			Files.walkFileTree(pagesRootPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					String path = file.toString();
					Matcher matcher = urlPattern.matcher(path);
					if (matcher.find()) {
						count.inc();
						pageRenderer.render(MAIN_PAGE, matcher.group(1))
								.whenComplete(() -> {
									if (count.dec() == 0) logger.info("End caching");
								});
					}
					return CONTINUE;
				}
			});
			return Promise.complete();
		} catch (IOException e) {
			return Promise.ofException(e);
		}
	}

	@Override
	public @NotNull MaterializedPromise<?> stop() {
		return Promise.complete();
	}
}
