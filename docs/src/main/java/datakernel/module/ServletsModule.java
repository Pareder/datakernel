package datakernel.module;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import datakernel.render.PageRenderer;
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

import java.util.Objects;
import java.util.concurrent.Executor;

import static io.datakernel.http.AsyncServletDecorator.mapException;
import static io.datakernel.http.HttpResponse.redirect302;

@SuppressWarnings("SameParameterValue")
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
