package io.datakernel.automation;

import com.github.mustachejava.Mustache;
import io.datakernel.async.Promise;
import io.datakernel.config.Config;
import io.datakernel.exception.UncheckedException;
import io.datakernel.http.AsyncServlet;
import io.datakernel.http.HttpRequest;
import io.datakernel.http.HttpResponse;
import io.datakernel.writer.ByteBufWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static java.nio.file.FileVisitResult.CONTINUE;

public class AutomationCheckerServlet implements AsyncServlet {
	private final Path pagesRootPath;
	private final Executor executor;
	private final List<PageChecker> pageCheckers;
	private final Mustache page;

	private AutomationCheckerServlet(Path pagesRootPath, Executor executor, List<PageChecker> pageCheckers, Mustache page) {
		this.pagesRootPath = pagesRootPath;
		this.executor = executor;
		this.pageCheckers = pageCheckers;
		this.page = page;
	}

	public static AutomationCheckerServlet create(Executor executor, Config config,
												  List<PageChecker> pageCheckers, Mustache page) {
		return new AutomationCheckerServlet(Paths.get(config.get("sourceFile.path")), executor, pageCheckers, page);
	}

	@Override
	public @NotNull Promise<HttpResponse> serve(@NotNull HttpRequest request) throws UncheckedException {
		return Promise.ofBlockingCallable(executor,
				() -> {
					Map<String, List<Throwable>> pathToException = new HashMap<>();
					PageVisitor pageVisitor = new PageVisitor(pathToException);
					Files.walkFileTree(pagesRootPath, pageVisitor);
					ByteBufWriter writer = new ByteBufWriter();
					page.execute(writer, pathToException.entrySet());
					return HttpResponse.ok200()
							.withBody(writer.getBuf());
				});
	}

	private final class PageVisitor extends SimpleFileVisitor<Path> {
		private Map<String, List<Throwable>> pathToExceptions;

		PageVisitor(Map<String, List<Throwable>> pathToExceptions) {
			this.pathToExceptions = pathToExceptions;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
			String fileName = path.toFile().getName();
			try {
				String content = new String(Files.readAllBytes(path));
				StringBuilder stringBuilder = new StringBuilder(content);
				pageCheckers.forEach(pageChecker -> pageChecker.check(stringBuilder, fileName)
						.forEach((key, value) -> {
							List<Throwable> exceptions = pathToExceptions.getOrDefault(key, new ArrayList<>());
							exceptions.addAll(value);
							pathToExceptions.putIfAbsent(key, exceptions);
						}));
			} catch (IOException e) {
				List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
				exceptions.add(e);
				pathToExceptions.putIfAbsent(fileName, exceptions);
			}
			return CONTINUE;
		}
	}
}
