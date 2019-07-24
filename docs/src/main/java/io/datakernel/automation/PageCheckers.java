package io.datakernel.automation;

import io.datakernel.config.Config;
import io.datakernel.dao.ResourceDao;

public final class PageCheckers {
	public static PageChecker linkChecker(Config config) {
		return PageLinkChecker.create(config);
	}

	public static PageChecker githubIncludeChecker(ResourceDao resourceDao) {
		return PageGithubTagIncludeChecker.create(resourceDao);
	}

	public static PageChecker includeChecker(ResourceDao resourceDao) {
		return PageIncludeChecker.create(resourceDao);
	}

	public static PageChecker paragraphChecker() {
		return PageParagraphChecker.create();
	}

	public static PageChecker staticChecker() {
		return PageStaticChecker.create();
	}
}
