package io.datakernel.automation;

import io.datakernel.dao.ResourceDao;
import io.datakernel.tag.ReplaceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.datakernel.tag.GithubIncludeReplacer.GITHUB_INCLUDE_TAG;
import static io.datakernel.tag.GithubIncludeReplacer.START_END_TAG;
import static java.lang.String.format;
import static java.util.regex.Pattern.DOTALL;

public class PageGithubTagIncludeChecker implements PageChecker {
	private final Pattern githubTagPattern = Pattern.compile(GITHUB_INCLUDE_TAG);
	private final ResourceDao resourceDao;

	private PageGithubTagIncludeChecker(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
	}

	public static PageGithubTagIncludeChecker create(ResourceDao resourceDao) {
		return new PageGithubTagIncludeChecker(resourceDao);
	}

	@Override
	public Map<String, List<Throwable>> check(StringBuilder content, String fileName) {
		Map<String, List<Throwable>> pathToExceptions = new HashMap<>();
		try {
			Matcher match = githubTagPattern.matcher(content.toString());
			while (match.find()) {
				String resourceName = match.group(1);
				String tag = match.group(3);

				String includeContent = resourceDao.getResource(resourceName);
				if (tag != null) {
					Pattern pattern = Pattern.compile(format(START_END_TAG, tag), DOTALL);
					Matcher matchStartEndTag = pattern.matcher(includeContent);
					if (!matchStartEndTag.find()) {
						List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
						exceptions.add(new PageException(format("Cannot find tag in match: %s", match.group())));
						pathToExceptions.putIfAbsent(fileName, exceptions);
					}
				}
			}
		} catch (IOException e) {
			List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
			exceptions.add(e);
			pathToExceptions.putIfAbsent(fileName, exceptions);
		}
		return pathToExceptions;
	}
}
