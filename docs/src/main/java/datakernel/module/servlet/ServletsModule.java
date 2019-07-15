package datakernel.module.servlet;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.vladsch.flexmark.ext.admonition.AdmonitionExtension;
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import datakernel.dao.PageDao;
import io.datakernel.async.Promise;
import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.AbstractModule;
import io.datakernel.http.*;
import io.datakernel.loader.StaticLoader;
import io.datakernel.writer.ByteBufWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;

import static io.datakernel.http.AsyncServletDecorator.mapHttpException;
import static io.datakernel.util.CollectionUtils.map;
import static java.util.Arrays.asList;

@SuppressWarnings("SameParameterValue")
public final class ServletsModule extends AbstractModule {
	private static final String MAIN_SECTOR = "main";
	private static final String INDEX_DOC = "index";

	private static Promise<ByteBuf> renderPage(HtmlRenderer renderer, PageDao pageDao, Parser parser, Mustache page,
											   @NotNull String sector, @Nullable String destination, @NotNull String doc) {
		return pageDao.loadPage(sector, destination, doc)
				.then(pageView -> pageView == null ?
						Promise.ofException(HttpException.badRequest400()) :
						Promise.of(pageView))
				.map(pageContent -> {
					Document document = parser.parse(pageContent.getPageContent());
					String renderedContent = renderer.render(document);
					ByteBufWriter writer = new ByteBufWriter();
					page.execute(writer, map("page", pageContent.setContent(renderedContent)));
					return writer.getBuf();
				});
	}

	@Provides
	MutableDataSet options() {
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, asList(
				AdmonitionExtension.create(),
				TablesExtension.create(),
				JekyllFrontMatterExtension.create(),
				JekyllTagExtension.create()));
		return options;
	}

	@Provides
	StaticLoader staticLoader(Executor executor, @Named("properties") Config config) {
		return StaticLoader.ofClassPath(executor, config.get("templates.path"));
	}

	@Provides
	HtmlRenderer htmlRenderer(MutableDataSet options) {
		return HtmlRenderer.builder(options).build();
	}

	@Provides
	Parser markdownParser(MutableDataSet options) {
		return Parser.builder(options).build();
	}

	@Provides
	MustacheFactory mustache() {
		return new DefaultMustacheFactory();
	}

	@Provides
	AsyncServlet rootServlet(@Named("topic") AsyncServlet topicServlet,
							 @Named("home") AsyncServlet homeServlet,
							 @Named("doc") AsyncServlet docServlet,
							 @Named("fail") AsyncServlet failServlet,
							 @Named("static") AsyncServlet staticServlet) {
		return mapHttpException(failServlet)
				.serve(RoutingServlet.create()
						.map("/", homeServlet)
						.map("/static/*", staticServlet)
						.map("/:sector", topicServlet)
						.map("/:sector/:dest/:doc", docServlet));
	}

	@Provides
	@Named("static")
	AsyncServlet staticServlet(@Named("properties") Config config, Executor executor) {
		return StaticServlet.ofClassPath(executor, config.get("static.path"));
	}

	@Provides
	@Named("fail")
	AsyncServlet failServlet(@Named("properties") Config config, Executor executor, StaticLoader loader) {
		return StaticServlet.ofClassPath(executor, config.get("templates.path"))
				.withMappingTo("/404.html");
	}

	@Provides
	@Named("home")
	AsyncServlet homeServlet(@Named("properties") Config config,
							 PageDao pageDao,
							 Parser mdParser,
							 HtmlRenderer renderer,
							 MustacheFactory mustache) {
		Mustache page = mustache.compile(config.get("templates.path").concat("/index.html"));
		return request -> renderPage(renderer, pageDao, mdParser, page, MAIN_SECTOR, null, INDEX_DOC)
				.map(content -> HttpResponse.ok200().withBody(content));
	}

	@Provides
	@Named("topic")
	AsyncServlet sectorServlet(@Named("properties") Config config,
							   PageDao pageDao,
							   Parser mdParser,
							   HtmlRenderer renderer,
							   MustacheFactory mustache) {
		Mustache page = mustache.compile(config.get("templates.path").concat("/main.html"));
		return request -> {
			String sector = request.getPathParameter("sector");
			return renderPage(renderer, pageDao, mdParser, page, sector, null, INDEX_DOC)
					.map(content -> HttpResponse.ok200().withBody(content));
		};
	}

	@Provides
	@Named("doc")
	AsyncServlet docServlet(@Named("properties") Config config,
							PageDao pageDao,
							Parser parser,
							HtmlRenderer renderer,
							MustacheFactory factory) {
		Mustache page = factory.compile(config.get("templates.path").concat("/main.html"));
		return request -> {
			String doc = request.getPathParameter("doc");
			String destination = request.getPathParameter("dest");
			String sector = request.getPathParameter("sector");
			return renderPage(renderer, pageDao, parser, page, sector, destination, doc)
					.map(content -> HttpResponse.ok200().withBody(content));
		};
	}
}
