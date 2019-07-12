package datakernel;

import datakernel.module.database.MysqlDatabaseModule;
import datakernel.module.servlet.ServletsModule;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.Module;
import io.datakernel.di.module.Modules;
import io.datakernel.launchers.http.HttpServerLauncher;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static io.datakernel.config.Config.ofClassPathProperties;

public class AppLauncher extends HttpServerLauncher {
    @Provides
    @Named("properties")
    Config config() {
        return Config.create().overrideWith(ofClassPathProperties("app.properties"));
    }

    @Provides
    Executor executor() {
        return Executors.newCachedThreadPool();
    }

    @Override
    protected Module getBusinessLogicModule() {
        return Modules.combine(new ServletsModule(), new MysqlDatabaseModule());
    }

    public static void main(String[] args) throws Exception {
        HttpServerLauncher launcher = new AppLauncher();
        launcher.launch(args);
    }
}
