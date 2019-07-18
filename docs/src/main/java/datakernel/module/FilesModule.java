package datakernel.module;

import datakernel.dao.PageDao;
import datakernel.dao.FilePageDao;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.AbstractModule;

public final class FilesModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(PageDao.class).to(FilePageDao.class);
	}

	@Provides
	@Named("files")
	Config config() {
		return Config.ofClassPathProperties("files.properties");
	}
}
