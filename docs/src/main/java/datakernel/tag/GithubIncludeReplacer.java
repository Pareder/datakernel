package datakernel.tag;

import datakernel.dao.ResourceDao;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

public class GithubIncludeReplacer implements TagReplacer {
	private static final String GITHUB_INCLUDE_TAG = "\\{%\\s+github_sample\\s+(.+?)\\s+(tag:(.+?))?\\s+%}";
	private static final String START_END_TAG = "//\\[START\\s+(.+?)]\\n((\\t*).+?)\\s*//\\[END\\s+\\1]";
	private static final String EMPTY = "";
	private final ResourceDao resourceDao;
	private final Pattern githubTagPattern = Pattern.compile(GITHUB_INCLUDE_TAG);
	private final Pattern startEndTagPattern = Pattern.compile(START_END_TAG, DOTALL);

	public GithubIncludeReplacer(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
	}

	@Override
	public void replace(StringBuilder text) throws ReplaceException {
		try {
			Matcher matcher = githubTagPattern.matcher(text.toString());
			int offset = 0;
			while (matcher.find()) {
				String resourceName = matcher.group(1);
				String tag = matcher.group(3);
				String content = sliceStartEndTag(resourceDao.getResource(resourceName), tag);
				text.replace(matcher.start() + offset, matcher.end() + offset, content);
				offset += content.length() - (matcher.start() - matcher.end());
			}
		} catch (IOException e) {
			throw new ReplaceException(e);
		}
	}

	private String sliceStartEndTag(String content, String tag) {
		Matcher matcher = startEndTagPattern.matcher(content);
		while (matcher.find()) {
			String foundTag = matcher.group(1);
			if (foundTag.equals(tag)) {
				String redundantTabs = matcher.group(3);
				return matcher.group(2)
						.replaceAll(redundantTabs, EMPTY);
			}
		}
		return content;
	}
}
