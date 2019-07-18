package datakernel.module;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import datakernel.dao.FileResourceDao;
import datakernel.dao.PageDao;
import datakernel.dao.ResourceDao;
import datakernel.render.PageCacheImpl;
import datakernel.render.MustacheMarkdownPageRenderer;
import datakernel.render.PageCache;
import datakernel.render.PageRenderer;
import datakernel.tag.TagReplacer;
import io.datakernel.async.Promise;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.AbstractModule;
import io.datakernel.http.AsyncServlet;
import io.datakernel.http.HttpResponse;
import io.datakernel.http.RoutingServlet;
import io.datakernel.http.StaticServlet;
import io.datakernel.loader.StaticLoader;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import static datakernel.tag.TagReplacers.*;
import static io.datakernel.config.ConfigConverters.ofInteger;
import static io.datakernel.config.ConfigConverters.ofPath;
import static io.datakernel.http.AsyncServletDecorator.mapException;
import static io.datakernel.http.HttpResponse.redirect302;
import static java.util.Arrays.asList;

@SuppressWarnings("SameParameterValue")
public final class ServletsModule extends AbstractModule {
	private static final String MAIN_SECTOR = "main";
	private static final String INDEX_DOC = "index";

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
	@Named("projectSource")
	ResourceDao projectSourceDao(@Named("files") Config config) {
		return new FileResourceDao(config.get(ofPath(), "projectSourceFile.path"));
	}

	@Provides
	@Named("includes")
	ResourceDao includesDao(@Named("files") Config config) {
		return new FileResourceDao(config.get(ofPath(), "includes.path"));
	}

	@Provides
	List<TagReplacer> tagReplacer(@Named("includes") ResourceDao includesDao,
								  @Named("projectSource") ResourceDao projectSourceDao) {
		return asList(
				githubIncludeReplacer(projectSourceDao),
				includeReplacer(includesDao),
				highlightReplacer());
	}

	@Provides
	PageCache pageCache(Config config) {
		return new PageCacheImpl(
				config.get(ofInteger(), "amountBuffer", 1 << 8),
				config.get(ofInteger(), "amountBuffer", 1 << 16));
	}

	@Provides
	PageRenderer render(List<TagReplacer> replacer, Executor executor, PageDao pageDao,
						Parser parser, HtmlRenderer renderer, PageCache pageCache) {
		return new MustacheMarkdownPageRenderer(replacer, pageDao, parser, renderer, executor, pageCache);
	}

	@Provides
	MutableDataSet options() {
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, asList(
				JekyllFrontMatterExtension.create(),
				TablesExtension.create()));
		return options;
	}

	@Provides
	StaticLoader staticLoader(Executor executor, @Named("properties") Config config) {
		return StaticLoader.ofClassPath(executor, config.get("templates.path"));
	}

	@Provides
	AsyncServlet rootServlet(@Named("topic") AsyncServlet topicServlet,
							 @Named("home") AsyncServlet homeServlet,
							 @Named("doc") AsyncServlet docServlet,
							 @Named("fail") AsyncServlet failServlet,
							 @Named("static") AsyncServlet staticServlet) {
		return mapException(Objects::nonNull, failServlet)
				.serve(RoutingServlet.create()
						.map("/", homeServlet)
						.map("/favicon.ico", $ -> Promise.of(redirect302("/static")))
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
							 PageRenderer pageRenderer,
							 MustacheFactory mustache) {
		Mustache page = mustache.compile(config.get("templates.path").concat("/index.html"));
		return request -> pageRenderer.render(page, MAIN_SECTOR, null, INDEX_DOC)
				.map(content -> HttpResponse.ok200().withBody(content));
	}

	@Provides
	@Named("topic")
	AsyncServlet sectorServlet(@Named("properties") Config config,
							   MustacheFactory mustache,
							   PageRenderer pageRenderer) {
		Mustache page = mustache.compile(config.get("templates.path").concat("/main.html"));
		return request -> {
			String sector = request.getPathParameter("sector");
			return pageRenderer.render(page, sector, null, INDEX_DOC)
					.map(content -> HttpResponse.ok200().withBody(content));
		};
	}

	@Provides
	@Named("doc")
	AsyncServlet docServlet(@Named("properties") Config config,
							PageRenderer pageRenderer,
							MustacheFactory factory) {
		Mustache page = factory.compile(config.get("templates.path").concat("/main.html"));
		return request -> {
			String doc = request.getPathParameter("doc");
			String destination = request.getPathParameter("dest");
			String sector = request.getPathParameter("sector");
			return pageRenderer.render(page, sector, destination, doc)
					.map(content -> HttpResponse.ok200().withBody(content));
		};
	}
}
