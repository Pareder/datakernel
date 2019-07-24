package io.datakernel.tag;

import io.datakernel.dao.ResourceDao;
import org.python.util.PythonInterpreter;

public abstract class TagReplacers {
	public static TagReplacer includeReplacer(ResourceDao resourceDao) {
		return IncludeReplacer.create(resourceDao);
	}

	public static TagReplacer githubIncludeReplacer(ResourceDao resourceDao) {
		return GithubIncludeReplacer.create(resourceDao);
	}

	public static TagReplacer highlightReplacer(PythonInterpreter pythonInterpreter) {
		return HighlightReplacer.create(pythonInterpreter);
	}
}
