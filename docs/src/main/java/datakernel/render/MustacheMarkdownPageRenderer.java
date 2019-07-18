package datakernel.render;

import com.github.mustachejava.Mustache;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import datakernel.dao.PageDao;
import datakernel.tag.TagReplacer;
import io.datakernel.async.Promise;
import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.http.HttpException;
import io.datakernel.writer.ByteBufWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Executor;

import static io.datakernel.util.CollectionUtils.map;
import static io.datakernel.util.Preconditions.checkArgument;

public class MustacheMarkdownPageRenderer implements PageRenderer {
	private final PageDao pageDao;
	private final HtmlRenderer renderer;
	private final Parser parser;
	private final List<TagReplacer> replacers;
	private final Executor executor;
	private final PageCache pageCache;

	public MustacheMarkdownPageRenderer(List<TagReplacer> replacers, PageDao pageDao, Parser parser,
										HtmlRenderer renderer, Executor executor, PageCache pageCache) {
		checkArgument(!replacers.isEmpty(), () -> "List replacers cannot be empty");
		this.replacers = replacers;
		this.pageDao = pageDao;
		this.renderer = renderer;
		this.parser = parser;
		this.executor = executor;
		this.pageCache = pageCache;
	}

	public Promise<ByteBuf> render(Mustache page, @NotNull String sector, @Nullable String destination, @NotNull String doc) {
		ByteBuf cachedPage = pageCache.get(sector, destination, doc);
		return cachedPage != null ?
				Promise.of(cachedPage) :
				pageDao.loadPage(sector, destination, doc)
						.then(pageView -> pageView == null ?
								Promise.ofException(HttpException.badRequest400()) :
								Promise.ofBlockingCallable(executor, () -> {
									StringBuilder content = new StringBuilder(pageView.getPageContent());
									for (TagReplacer replacer : replacers) {
										replacer.replace(content);
									}
									Document document = parser.parse(content.toString());
									String renderedContent = renderer.render(document);

									ByteBufWriter writer = new ByteBufWriter();
									page.execute(writer, map("page", pageView.setRenderedContent(renderedContent)));
									return writer.getBuf();
								}))
						.whenResult(buf -> pageCache.put(sector, destination, doc, buf));
	}
}
