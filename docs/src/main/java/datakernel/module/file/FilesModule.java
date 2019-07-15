package datakernel.module.file;

import datakernel.dao.PageDao;
import datakernel.dao.PageFilesDao;
import io.datakernel.config.Config;
import io.datakernel.di.annotation.Named;
import io.datakernel.di.annotation.Provides;
import io.datakernel.di.module.AbstractModule;

public final class FilesModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(PageDao.class).to(PageFilesDao.class);
	}

	@Provides
	@Named("files")
	Config config() {
		return Config.ofClassPathProperties("files.properties");
	}
}
