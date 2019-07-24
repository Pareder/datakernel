package io.datakernel;

import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.Module;
import io.datakernel.di.module.Modules;
import io.datakernel.launchers.http.HttpServerLauncher;
import io.datakernel.module.ServletsModule;

import java.util.concurrent.Executor;

import static io.datakernel.config.Config.ofClassPathProperties;
import static java.lang.System.getProperties;
import static java.util.concurrent.Executors.newCachedThreadPool;

@SuppressWarnings("unused")
public class AppLauncher extends HttpServerLauncher {
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

	@Override
	protected Module getBusinessLogicModule() {
		return Modules.combine(new ServletsModule());
	}

	public static void main(String[] args) throws Exception {
		HttpServerLauncher launcher = new AppLauncher();
		launcher.launch(args);
	}
}
