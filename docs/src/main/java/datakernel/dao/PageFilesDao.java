package datakernel.dao;

import datakernel.model.PageView;
import io.datakernel.async.Promise;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Inject;
import io.datakernel.di.annotation.Named;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Executor;

import static io.datakernel.loader.StaticLoader.ofPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.FileVisitResult.CONTINUE;

public final class PageFilesDao implements PageDao {
	private static final String EMPTY = "";
	private static final String DOT = ".";
	private final String path;
	private final String extension;
	private final Executor executor;

	@Inject
	public PageFilesDao(@Named("files") Config config, Executor executor) {
		this.executor = executor;
		this.extension = config.get("sourceFile.extension");
		this.path = config.get("sourceFile.path");
	}

	@Override
	public Promise<PageView> loadPage(@NotNull String sector, @Nullable String destination, @NotNull String doc) {
		Path sectorPath = Paths.get(this.path + "/" + sector);
		return ofPath(executor, sectorPath)
				.load(resolveResource(destination, doc))
				.map(buf -> new PageView(sector, buf.asString(defaultCharset())))
				.then(pageView -> Promise.ofBlockingCallable(executor, () -> {
					Files.walkFileTree(sectorPath, new PageVisitor(sector, pageView));
					return pageView;
				}));
	}

	private String resolveResource(@Nullable String destination, @NotNull String doc) {
		String resource = (destination != null ? destination : "");
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
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			String destinationName = file.getParent().getFileName().toString();
			if (!sector.equals(destinationName)) {
				String docName = file.getFileName().toString().replace(DOT + extension, EMPTY);
				pageView.put(
						docName,
						docName,
						destinationName,
						destinationName);
			}
			return CONTINUE;
		}
	}
}
