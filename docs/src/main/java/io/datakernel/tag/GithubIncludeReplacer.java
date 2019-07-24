package io.datakernel.tag;

import io.datakernel.dao.ResourceDao;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;

public class GithubIncludeReplacer implements TagReplacer {
	public static final String GITHUB_INCLUDE_TAG = "\\{%\\s+github_sample\\s+(.+?)\\s+(tag:(.+?))?\\s*%}";
	public static final String START_END_TAG = "\\/\\/\\s*\\[START\\s+%1$s\\]\\n((\\t*).+?)\\s*\\/\\/\\s*\\[END\\s+%1$s\\]";
	private static final String EMPTY = "";
	private final ResourceDao resourceDao;
	private final Pattern githubTagPattern = Pattern.compile(GITHUB_INCLUDE_TAG);

	private GithubIncludeReplacer(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
	}

	public static GithubIncludeReplacer create(ResourceDao resourceDao) {
		return new GithubIncludeReplacer(resourceDao);
	}

	@Override
	public void replace(StringBuilder text) throws ReplaceException {
		try {
			Matcher match = githubTagPattern.matcher(text.toString());
			int offset = 0;
			while (match.find()) {
				String resourceName = match.group(1);
				String tag = match.group(3);

				String content = resourceDao.getResource(resourceName);
				if (tag != null) {
					Pattern pattern = Pattern.compile(format(START_END_TAG, tag), DOTALL);
					Matcher matchStartEndTag = pattern.matcher(content);
					if (matchStartEndTag.find()) {
						content = Pattern.compile('^' + matchStartEndTag.group(2), MULTILINE)
								.matcher(matchStartEndTag.group(1))
								.replaceAll(EMPTY);
					}
				}
				text.replace(match.start() + offset, match.end() + offset, content);
				offset += content.length() - (match.end() - match.start());
			}
		} catch (IOException e) {
			throw new ReplaceException(e);
		}
	}
}
