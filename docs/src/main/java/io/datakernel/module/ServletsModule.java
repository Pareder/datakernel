package io.datakernel.module;

import io.datakernel.async.Promise;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.AbstractModule;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.http.AsyncServlet;
import io.datakernel.http.HttpResponse;
import io.datakernel.http.RoutingServlet;
import io.datakernel.http.StaticServlet;
import io.datakernel.loader.StaticLoader;
import io.datakernel.render.PageRenderer;
import io.datakernel.render.PreCachingService;

import java.util.concurrent.Executor;

import static io.datakernel.config.ConfigConverters.ofPath;
import static io.datakernel.http.AsyncServletDecorator.mapException;
import static io.datakernel.http.HttpResponse.notFound404;
import static io.datakernel.http.HttpResponse.redirect302;

@SuppressWarnings({"SameParameterValue", "unused", "WeakerAccess"})
public final class ServletsModule extends AbstractModule {
	public static final String MAIN_PAGE = "main.html";
	public static final String INDEX_PAGE = "index.html";

	@Override
	protected void configure() {
		install(new RenderModule());
	}

	@Provides
	PreCachingService preCachingService(Eventloop eventloop, @Named("properties") Config config,
										PageRenderer pageRenderer) {
		return PreCachingService.create(eventloop, config, pageRenderer);
	}

	@Provides
	StaticLoader staticLoader(Executor executor, @Named("properties") Config config) {
		return StaticLoader.ofClassPath(executor, config.get("templates.path"));
	}

	@Provides
	AsyncServlet rootServlet(@Named("home") AsyncServlet homeServlet,
							 @Named("main") AsyncServlet mainServlet,
							 @Named("fail") AsyncServlet failServlet,
							 @Named("static") AsyncServlet staticServlet) {
		return mapException(e -> false, failServlet)
				.serve(RoutingServlet.create()
						.map("/", homeServlet)
						.map("/*", mainServlet)
						.map("/static/*", staticServlet)
						.map("/favicon.ico", staticServlet));
	}

	@Provides
	@Named("static")
	AsyncServlet staticServlet(@Named("properties") Config config, Executor executor) {
		return StaticServlet.ofPath(executor, config.get(ofPath(), "static.path"));
	}

	@Provides
	@Named("fail")
	AsyncServlet failServlet(@Named("properties") Config config, StaticLoader loader) {
		return request -> loader.load("/404.html")
				.map(body -> notFound404().withBody(body));
	}

	@Provides
	@Named("home")
	AsyncServlet homeServlet(PageRenderer pageRenderer) {
		return request -> pageRenderer.render(INDEX_PAGE, request.getPath())
				.map(content -> HttpResponse.ok200().withBody(content));
	}

	@Provides
	@Named("main")
	AsyncServlet mainServlet(PageRenderer pageRenderer) {
		return request -> pageRenderer.render(MAIN_PAGE, request.getPath())
				.map(content -> HttpResponse.ok200().withBody(content));
	}
}
