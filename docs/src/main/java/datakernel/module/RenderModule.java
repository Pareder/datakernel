package datakernel.module;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import datakernel.dao.FilePageDao;
import datakernel.dao.FileResourceDao;
import datakernel.dao.PageDao;
import datakernel.dao.ResourceDao;
import datakernel.render.MustacheMarkdownPageRenderer;
import datakernel.render.PageCache;
import datakernel.render.PageCacheImpl;
import datakernel.render.PageRenderer;
import datakernel.tag.OrderedTagReplacer;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.AbstractModule;
import org.python.util.PythonInterpreter;

import java.util.List;
import java.util.concurrent.Executor;

import static com.vladsch.flexmark.parser.Parser.EXTENSIONS;
import static com.vladsch.flexmark.parser.ParserEmulationProfile.GITHUB_DOC;
import static datakernel.tag.TagReplacers.*;
import static io.datakernel.config.ConfigConverters.ofInteger;
import static io.datakernel.config.ConfigConverters.ofPath;
import static java.util.Arrays.asList;

public class RenderModule extends AbstractModule {
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
	ResourceDao projectSourceDao(@Named("properties") Config config) {
		return new FileResourceDao(config.get(ofPath(), "projectSourceFile.path"));
	}

	@Provides
	@Named("includes")
	ResourceDao includesDao(@Named("properties") Config config) {
		return new FileResourceDao(config.get(ofPath(), "includes.path"));
	}

	@Override
	protected void configure() {
		bind(PageDao.class).to(FilePageDao.class);
	}

	@Provides
	List<OrderedTagReplacer> tagReplacer(@Named("includes") ResourceDao includesDao,
										 @Named("projectSource") ResourceDao projectSourceDao,
										 PythonInterpreter pythonInterpreter) {
		return asList(
				new OrderedTagReplacer(1, githubIncludeReplacer(projectSourceDao)),
				new OrderedTagReplacer(2, includeReplacer(includesDao)),
				new OrderedTagReplacer(3, highlightReplacer(pythonInterpreter)));
	}

	@Provides
	PageCache pageCache(Config config) {
		return new PageCacheImpl(
				config.get(ofInteger(), "amountBuffer", 1 << 8),
				config.get(ofInteger(), "amountBuffer", 1 << 20));
	}

	@Provides
	PythonInterpreter pythonInterpreter() {
		return new PythonInterpreter();
	}

	@Provides
	PageRenderer render(List<OrderedTagReplacer> replacer, Executor executor, PageDao pageDao,
						Parser parser, HtmlRenderer renderer, PageCache pageCache) {
		return new MustacheMarkdownPageRenderer(replacer, pageDao, parser, renderer, executor, pageCache);
	}

	@Provides
	MutableDataSet options() {
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.PARSER_EMULATION_PROFILE, GITHUB_DOC);
		options.set(EXTENSIONS, asList(
				JekyllFrontMatterExtension.create(),
				TablesExtension.create()));
		return options;
	}
}
