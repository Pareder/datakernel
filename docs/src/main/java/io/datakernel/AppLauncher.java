package io.datakernel;

import com.kenai.jaffl.annotations.In;
import io.datakernel.automation.AutomationService;
import io.datakernel.automation.PageChecker;
import io.datakernel.config.Config;
import io.datakernel.dao.ResourceDao;
import io.datakernel.di.annotation.Inject;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.Module;
import io.datakernel.di.module.Modules;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.launchers.http.HttpServerLauncher;
import io.datakernel.module.ServletsModule;
import io.datakernel.render.PreCachingService;
import io.datakernel.service.Service;

import java.util.List;
import java.util.concurrent.Executor;

import static io.datakernel.automation.PageCheckers.*;
import static io.datakernel.config.Config.ofClassPathProperties;
import static java.lang.System.getProperties;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newCachedThreadPool;

@SuppressWarnings("unused")
public class AppLauncher extends HttpServerLauncher {
	@Inject
	PreCachingService preCachingService;

	@Provides
	@Named("properties")
	Config config() {
		return Config.create().overrideWith(ofClassPathProperties("app.properties"))
				.overrideWith(Config.ofProperties(getProperties())
						.getChild("config"));
	}

	@Provides
	Executor executor() {
		return newCachedThreadPool();
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
	AutomationService automationService(Eventloop eventloop, Executor executor,
										@Named("properties") Config config, List<PageChecker> pageCheckers) {
		return AutomationService.create(eventloop, executor, config, pageCheckers);
	}

	@Override
	protected Module getBusinessLogicModule() {
		return Modules.combine(new ServletsModule());
	}

	public static void main(String[] args) throws Exception {
		HttpServerLauncher launcher = new AppLauncher();
		launcher.launch(args);
	}
}
