package datakernel.tag;

import datakernel.dao.ResourceDao;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;

public class GithubIncludeReplacer implements TagReplacer {
	private static final String GITHUB_INCLUDE_TAG = "\\{%\\s+github_sample\\s+(.+?)\\s+(tag:(.+?))?\\s*%}";
	private static final String START_END_TAG = "//\\[START\\s+%1$s]\\n((\\t*).+?)\\s*//\\[END\\s+%1$s]";
	private static final String EMPTY = "";
	private final ResourceDao resourceDao;

	private final Pattern githubTagPattern = Pattern.compile(GITHUB_INCLUDE_TAG);

	public GithubIncludeReplacer(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
	}

	@Override
	public void replace(StringBuilder text) throws ReplaceException {
		try {
			Matcher match = githubTagPattern.matcher(text.toString());
			int offset = 0;
			while (match.find()) {
				String resourceName = match.group(1);
				String tag = match.group(3);

				String content = sliceStartEndTag(resourceDao.getResource(resourceName), tag);
				text.replace(match.start() + offset, match.end() + offset, content);
				offset += content.length() - (match.end() - match.start());
			}
		} catch (IOException e) {
			throw new ReplaceException(e);
		}
	}

	private String sliceStartEndTag(String content, String tag) {
		Pattern pattern = Pattern.compile(format(START_END_TAG, tag), DOTALL);
		Matcher matcher = pattern.matcher(content);
		if (matcher.find()) {
			return Pattern.compile('^' + matcher.group(2), MULTILINE)
						.matcher(matcher.group(1))
						.replaceAll(EMPTY);
		}
		return content;
	}
}
