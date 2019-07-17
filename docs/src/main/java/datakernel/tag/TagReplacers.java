package datakernel.tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

public abstract class TagReplacers {
	private static final String GITHUB_INCLUDE_TAG = "\\{%\\s+github_sample\\s+(.+?)\\s+(.+?)\\s+%}";
	private static final String HIGHLIGHT_TAG = "\\{%\\s+highlight\\s+(\\w+)\\s+%}(.*?)\\{%\\s+endhighlight\\s+%}";
	private static final String CODE_BLOCK_BREAKERS = "```";


	public static TagReplacer includeReplacer(Path rootPath) {
		return new IncludeReplacer(rootPath);
	}

	public static TagReplacer githubIncludeReplacer(Path rootPath) {
		Pattern pattern = Pattern.compile(GITHUB_INCLUDE_TAG);
		return text -> {
			try {
				Matcher matcher = pattern.matcher(text.toString());
				Path resourcePath;
				int offset = 0;
				while (matcher.find()) {
					String path = matcher.group(1);
					resourcePath = rootPath.resolve(path);
					String content = new String(Files.readAllBytes(resourcePath));
					text.replace(matcher.start() + offset, matcher.end() + offset, content);
					offset += content.length() - (matcher.start() - matcher.end());
				}
			} catch (IOException e) {
				throw new ReplaceException(e);
			}
		};
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
