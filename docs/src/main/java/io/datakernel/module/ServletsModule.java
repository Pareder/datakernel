package io.datakernel.module;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.datakernel.async.Promise;
import io.datakernel.automation.PageCheckers;
import io.datakernel.config.Config;
import io.datakernel.dao.ResourceDao;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.AbstractModule;
import io.datakernel.http.AsyncServlet;
import io.datakernel.http.HttpResponse;
import io.datakernel.http.RoutingServlet;
import io.datakernel.http.StaticServlet;
import io.datakernel.loader.StaticLoader;
import io.datakernel.automation.AutomationCheckerServlet;
import io.datakernel.automation.PageChecker;
import io.datakernel.render.PageRenderer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import static io.datakernel.automation.PageCheckers.*;
import static io.datakernel.http.AsyncServletDecorator.mapException;
import static io.datakernel.http.HttpResponse.notFound404;
import static io.datakernel.http.HttpResponse.redirect302;
import static java.util.Arrays.asList;

@SuppressWarnings({"SameParameterValue", "unused"})
public final class ServletsModule extends AbstractModule {
	private static final String HOME_SECTOR = "home";
	private static final String INDEX_DOC = "index";

	@Override
	protected void configure() {
		install(new RenderModule());
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
							 @Named("static") AsyncServlet staticServlet,
							 @Named("automation") AsyncServlet automationServlet) {
		return mapException(Objects::nonNull, failServlet)
				.serve(RoutingServlet.create()
						.map("/", homeServlet)
						.map("/favicon.ico", $ -> Promise.of(redirect302("/static")))
						.map("/static/*", staticServlet)
						.map("/:sector", topicServlet)
						.map("/automation/", automationServlet)
						.map("/:sector/:dest/:doc", docServlet));
	}

	@Provides
	List<PageChecker> pageCheckers(@Named("properties") Config config,
								   @Named("includes") ResourceDao includesDao,
								   @Named("projectSource") ResourceDao projectSourceDao) {
		return asList(
				linkChecker(config),
				githubIncludeChecker(projectSourceDao),
				includeChecker(includesDao),
				paragraphChecker(),
				staticChecker()
		);
	}

	@Provides
	@Named("automation")
	AsyncServlet automationServlet(@Named("properties") Config config, Executor executor,
								   List<PageChecker> pageCheckers, MustacheFactory mustacheFactory) {
		Mustache mustache = mustacheFactory.compile(config.get("templates.path").concat("/automationPage.html"));
		return AutomationCheckerServlet.create(executor, config, pageCheckers, mustache);
	}

	@Provides
	@Named("static")
	AsyncServlet staticServlet(@Named("properties") Config config, Executor executor) {
		return StaticServlet.ofClassPath(executor, config.get("static.path"));
	}

	@Provides
	@Named("fail")
	AsyncServlet failServlet(@Named("properties") Config config, Executor executor, StaticLoader loader) {
		return request -> loader.load("/404.html")
				.map(body -> notFound404().withBody(body));
	}

	@Provides
	@Named("home")
	AsyncServlet homeServlet(@Named("properties") Config config,
							 PageRenderer pageRenderer,
							 MustacheFactory mustache) {
		Mustache page = mustache.compile(config.get("templates.path").concat("/index.html"));
		return request -> pageRenderer.render(page, HOME_SECTOR, null, INDEX_DOC)
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
