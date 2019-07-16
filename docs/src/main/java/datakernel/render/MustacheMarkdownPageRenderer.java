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

import java.util.Iterator;
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

	public MustacheMarkdownPageRenderer(List<TagReplacer> replacers, PageDao pageDao, Parser parser,
										HtmlRenderer renderer, Executor executor) {
		checkArgument(!replacers.isEmpty(), () -> "List replacers cannot be emty");
		this.replacers = replacers;
		this.pageDao = pageDao;
		this.renderer = renderer;
		this.parser = parser;
		this.executor = executor;
	}

	public Promise<ByteBuf> render(Mustache page, @NotNull String sector, @Nullable String destination, @NotNull String doc) {
		return pageDao.loadPage(sector, destination, doc)
				.then(pageView -> pageView == null ?
						Promise.ofException(HttpException.badRequest400()) :
						Promise.ofBlockingCallable(executor,
								() -> {
									Document document = parser.parse(pageView.getPageContent());
									return renderer.render(document);
								})
								.then(renderedContent -> processByQueue(replacers.iterator(), renderedContent))
								.map(replacedContent -> {
									ByteBufWriter writer = new ByteBufWriter();
									page.execute(writer, map("page", pageView.setContent(replacedContent)));
									return writer.getBuf();
								}));
	}

	private Promise<String> processByQueue(Iterator<TagReplacer> iterator, String text) {
		return !iterator.hasNext() ? Promise.of(text) : iterator.next().replace(text)
				.whenResult(res -> processByQueue(iterator, res));
	}

}
