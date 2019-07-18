package datakernel.tag;

import datakernel.dao.ResourceDao;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

public abstract class TagReplacers {
	private static final String HIGHLIGHT_TAG = "\\{%\\s+highlight\\s+(\\w+)\\s+%}(.*?)\\{%\\s+endhighlight\\s+%}";
	private static final String CODE_BLOCK_BREAKERS = "```";

	public static TagReplacer includeReplacer(ResourceDao resourceDao) {
		return new IncludeReplacer(resourceDao);
	}

	public static TagReplacer githubIncludeReplacer(ResourceDao resourceDao) {
		return new GithubIncludeReplacer(resourceDao);
	}

	public static TagReplacer highlightReplacer() {
		Pattern pattern = Pattern.compile(HIGHLIGHT_TAG, DOTALL);
		return text -> {
			Matcher matcher = pattern.matcher(text.toString());
			int offset = 0;
			while (matcher.find()) {
				String lang = matcher.group(1);
				String innerContent = matcher.group(2);
				String newContent = CODE_BLOCK_BREAKERS + lang
						+ innerContent +
						CODE_BLOCK_BREAKERS;
				text.replace(matcher.start() + offset, matcher.end() + offset, newContent);
				offset += newContent.length() - (matcher.end() - matcher.start());
			}
		};
	}
}
