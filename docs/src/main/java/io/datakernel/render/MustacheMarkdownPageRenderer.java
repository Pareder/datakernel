package io.datakernel.render;

import com.github.mustachejava.Mustache;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import io.datakernel.dao.PageDao;
import io.datakernel.tag.OrderedTagReplacer;
import io.datakernel.tag.TagReplacer;
import io.datakernel.async.Promise;
import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.http.HttpException;
import io.datakernel.writer.ByteBufWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.datakernel.util.CollectionUtils.map;
import static io.datakernel.util.Preconditions.checkArgument;
import static java.util.Comparator.comparingInt;
import static java.util.regex.Pattern.DOTALL;

public class MustacheMarkdownPageRenderer implements PageRenderer {
	private static final String TAG_PATTERN = "(\\{%\\s*highlight.*?%}\\n.*?\\n\\{%\\s*endhighlight\\s*%}|\\{%\\s*include.*?%}|\\{%\\s*github_sample.*?%})";
	private final PageDao pageDao;
	private final HtmlRenderer renderer;
	private final Parser parser;
	private final List<OrderedTagReplacer> replacers;
	private final Executor executor;
	private final PageCache pageCache;
	private final Pattern tagPattern = Pattern.compile(TAG_PATTERN, DOTALL);

	public MustacheMarkdownPageRenderer(List<OrderedTagReplacer> replacers, PageDao pageDao, Parser parser,
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
									Queue<String> savedTags = new LinkedList<>();
									String pageContent = pageView.getPageContent();
									saveTags(pageContent, savedTags);
									Document document = parser.parse(pageContent);
									String renderedContent = renderer.render(document);
									StringBuilder content = replaceToSavedTags(renderedContent, savedTags);
									replacers.sort(comparingInt(OrderedTagReplacer::getOrder));
									for (TagReplacer replacer : replacers) {
										replacer.replace(content);
									}

									ByteBufWriter writer = new ByteBufWriter();
									page.execute(writer, map(
											"page", pageView.setRenderedContent(content.toString()),
											"active", map(sector, true)));
									return writer.getBuf();
								}))
						.whenResult(buf -> pageCache.put(sector, destination, doc, buf));
	}

	private StringBuilder replaceToSavedTags(String renderedContent, Queue<String> savedTags) {
		StringBuilder resultBuilder = new StringBuilder(renderedContent);
		Matcher match = tagPattern.matcher(renderedContent);
		int offset = 0;
		while (match.find()) {
			String savedTag = savedTags.remove();
			resultBuilder.replace(match.start() + offset, match.end() + offset, savedTag);
			offset += savedTag.length() - (match.end() - match.start());
		}
		return resultBuilder;
	}

	private void saveTags(String pageContent, Queue<String> savedTags) {
		Matcher match = tagPattern.matcher(pageContent);
		while (match.find()) {
			savedTags.add(match.group());
		}
	}
}
