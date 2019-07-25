package io.datakernel.automation;

import io.datakernel.async.MaterializedPromise;
import io.datakernel.async.Promise;
import io.datakernel.async.Promises;
import io.datakernel.config.Config;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.eventloop.EventloopService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static java.nio.file.FileVisitResult.CONTINUE;

public final class AutomationService implements EventloopService {
	private final static Logger logger = LoggerFactory.getLogger(AutomationService.class);
	private final Path pagesRootPath;
	private final Executor executor;
	private final List<PageChecker> pageCheckers;
	private final Eventloop eventloop;

	private AutomationService(Eventloop eventloop, Path pagesRootPath, Executor executor, List<PageChecker> pageCheckers) {
		this.pagesRootPath = pagesRootPath;
		this.executor = executor;
		this.pageCheckers = pageCheckers;
		this.eventloop = eventloop;
	}

	public static AutomationService create(Eventloop eventloop, Executor executor, Config config, List<PageChecker> pageCheckers) {
		return new AutomationService(eventloop, Paths.get(config.get("caching.path")), executor, pageCheckers);
	}

	@Override
	public @NotNull Eventloop getEventloop() {
		return eventloop;
	}

	@Override
	public @NotNull MaterializedPromise<?> start() {
		logger.info("Starting checking");
		Map<String, List<Throwable>> pathToExceptions = new HashMap<>();
		return Promises.sequence(pageCheckers.stream().map(pageChecker ->
				Promise.ofBlockingRunnable(executor,
						() -> {
							try {
								PageVisitor pageVisitor = new PageVisitor(pathToExceptions, pageChecker);
								Files.walkFileTree(pagesRootPath, pageVisitor);
							} catch (IOException e) {
								List<Throwable> exceptions =
										pathToExceptions.getOrDefault(pagesRootPath.toString(), new ArrayList<>());
								exceptions.add(e);
								pathToExceptions.putIfAbsent(pagesRootPath.toString(), exceptions);
							}
						}))
				.iterator())
				.then($ -> {
					logger.info("End checking");
					pathToExceptions.forEach((path, execeptions) -> {
						System.out.println(path.toUpperCase());
						execeptions.forEach(System.out::println);
					});
					if (!pathToExceptions.isEmpty()) {
						return Promise.ofException(new AutomationValidationException());
					}
					return Promise.complete();
				})
				.materialize();
	}

	@Override
	public @NotNull MaterializedPromise<?> stop() {
		return Promise.complete();
	}

	private final class PageVisitor extends SimpleFileVisitor<Path> {
		private Map<String, List<Throwable>> pathToExceptions;
		private PageChecker pageChecker;

		PageVisitor(Map<String, List<Throwable>> pathToExceptions, PageChecker pageChecker) {
			this.pathToExceptions = pathToExceptions;
			this.pageChecker = pageChecker;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
			String fileName = path.toFile().getName();
			try {
				String content = new String(Files.readAllBytes(path));
				StringBuilder stringBuilder = new StringBuilder(content);
				pageChecker.check(stringBuilder, fileName).forEach((key, value) -> {
					List<Throwable> exceptions = pathToExceptions.getOrDefault(key, new ArrayList<>());
					exceptions.addAll(value);
					pathToExceptions.putIfAbsent(key, exceptions);
				});
			} catch (IOException e) {
				List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
				exceptions.add(e);
				pathToExceptions.putIfAbsent(fileName, exceptions);
			}
			return CONTINUE;
		}
	}
}
