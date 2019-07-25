package io.datakernel.render;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import io.datakernel.async.Promise;
import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.dao.PageDao;
import io.datakernel.dao.ResourceDao;
import io.datakernel.dao.ResourceResolver;
import io.datakernel.http.HttpException;
import io.datakernel.model.PageView;
import io.datakernel.tag.OrderedTagReplacer;
import io.datakernel.tag.ReplaceException;
import io.datakernel.tag.TagReplacer;
import io.datakernel.writer.ByteBufWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
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
	private final Pattern tagPattern = Pattern.compile(TAG_PATTERN, DOTALL);

	private final ResourceResolver<String, Path> resourceResolver;
	private final List<OrderedTagReplacer> replacers;
	private final MustacheFactory mustache;
	private final ResourceDao templateDao;
	private final HtmlRenderer renderer;
	private final PageCache pageCache;
	private final Executor executor;
	private final PageDao pageDao;
	private final Parser parser;

	public MustacheMarkdownPageRenderer(List<OrderedTagReplacer> replacers, PageDao pageDao, Parser parser,
										HtmlRenderer renderer, Executor executor, PageCache pageCache,
										MustacheFactory mustache, ResourceDao templateDao,
										ResourceResolver<String, Path> resourceResolver) {
		checkArgument(!replacers.isEmpty(), () -> "List replacers cannot be empty");
		this.resourceResolver = resourceResolver;
		this.templateDao = templateDao;
		this.replacers = replacers;
		this.pageCache = pageCache;
		this.renderer = renderer;
		this.mustache = mustache;
		this.executor = executor;
		this.pageDao = pageDao;
		this.parser = parser;
	}

	@Override
	public Promise<ByteBuf> render(@NotNull String template, @NotNull String url) {
		try {
			Path resource = resourceResolver.resolve(url);
			ByteBuf cachedPage = pageCache.get(resource.toString());
			return cachedPage != null ?
					Promise.of(cachedPage) :
					pageDao.loadPage(resource)
							.then(pageView -> pageView == null ?
									Promise.ofException(HttpException.badRequest400()) :
									Promise.ofBlockingCallable(executor, () -> {
										StringBuilder content = doRender(pageView);
										String replacedContent = replaceWithTags(content);

										StringReader templateReader = new StringReader(templateDao.getResource(template));
										Mustache mustache = this.mustache.compile(templateReader, template);
										ByteBufWriter writer = new ByteBufWriter();

										mustache.execute(writer, map(
												"page", pageView.setRenderedContent(replacedContent),
												"active", map(url, true)));
										return writer.getBuf();
									}))
							.whenResult(buf -> pageCache.put(resource.toString(), buf));
		} catch (IOException e) {
			return Promise.ofException(e);
		}
	}

	private String replaceWithTags(StringBuilder content) throws ReplaceException {
		replacers.sort(comparingInt(OrderedTagReplacer::getOrder));
		for (TagReplacer replacer : replacers) {
			replacer.replace(content);
		}
		return content.toString();
	}

	private StringBuilder doRender(PageView pageView) {
		Queue<String> savedTags = new LinkedList<>();
		String pageContent = pageView.getPageContent();
		saveTags(pageContent, savedTags);
		Document document = parser.parse(pageContent);
		String renderedContent = renderer.render(document);
		return replaceToSavedTags(renderedContent, savedTags);
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
