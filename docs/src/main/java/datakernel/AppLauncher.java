package datakernel;

import datakernel.module.ServletsModule;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.Module;
import io.datakernel.launchers.http.HttpServerLauncher;

import java.util.concurrent.Executor;

import static io.datakernel.config.Config.ofClassPathProperties;
import static io.datakernel.di.module.Modules.combine;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class AppLauncher extends HttpServerLauncher {
	@Provides
	@Named("properties")
	Config config() {
		return Config.create().overrideWith(ofClassPathProperties("app.properties"));
	}

	@Provides
	Executor executor() {
		return newCachedThreadPool();
	}

	@Override
	protected Module getBusinessLogicModule() {
		return combine(new ServletsModule());
	}

	public static void main(String[] args) throws Exception {
		HttpServerLauncher launcher = new AppLauncher();
		launcher.launch(args);
	}
}
