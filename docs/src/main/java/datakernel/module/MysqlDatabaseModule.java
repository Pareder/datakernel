package datakernel.module;

import com.mysql.cj.jdbc.MysqlDataSource;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.AbstractModule;

import javax.sql.DataSource;
import java.sql.SQLException;

import static io.datakernel.config.ConfigConverters.ofBoolean;


public final class MysqlDatabaseModule extends AbstractModule {
	@Provides
	DataSource dataSource(@Named("database") Config config) throws SQLException {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUrl(String.format("jdbc:mysql://%s:%s/%s",
				config.get("dataSource.serverName"),
				config.get("dataSource.port"),
				config.get("dataSource.databaseName")));
		dataSource.setUser(config.get("dataSource.user"));
		dataSource.setPassword(config.get("dataSource.password"));
		dataSource.setServerTimezone(config.get("dataSource.timeZone"));
		dataSource.setAllowMultiQueries(config.get(ofBoolean(), "dataSource.timeZone"));
		return dataSource;
	}

	@Provides
	@Named("database")
	Config config() {
		return Config.ofClassPathProperties("database.properties");
	}
}
