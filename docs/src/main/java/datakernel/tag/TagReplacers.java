package datakernel.tag;

import datakernel.dao.ResourceDao;
import org.python.util.PythonInterpreter;

public abstract class TagReplacers {
	public static TagReplacer includeReplacer(ResourceDao resourceDao) {
		return new IncludeReplacer(resourceDao);
	}

	public static TagReplacer githubIncludeReplacer(ResourceDao resourceDao) {
		return new GithubIncludeReplacer(resourceDao);
	}

	public static TagReplacer highlightReplacer(PythonInterpreter pythonInterpreter) {
		return new HighlightReplacer(pythonInterpreter);
	}
}
