package datakernel.tag;

import io.datakernel.async.Promise;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TagReplacers {
	private static final String INCLUDE_TAG = "";
	private static final String HIGHLIGHT_TAG = "";
	private static final String CODE_BLOCK_BREAKERS = "```";
	private static final String BREAK = "\n";

	public static TagReplacer includeReplacer(Path rootPath) {
		Pattern pattern = Pattern.compile(INCLUDE_TAG);
		return text -> {
			try {
				StringBuilder stringBuilder = new StringBuilder(text);
				Matcher matcher = pattern.matcher(text);
				Path resourcePath;
				while (matcher.find()) {
					String path = matcher.group(0);
					resourcePath = rootPath.resolve(path);
					stringBuilder.delete(matcher.start(), matcher.end())
							.insert(matcher.start(), Files.readAllBytes(resourcePath));
				}
				return Promise.of(stringBuilder.toString());
			} catch (IOException e) {
				return Promise.ofException(e);
			}
		};
	}

	public static TagReplacer highlightReplacer() {
		Pattern pattern = Pattern.compile(HIGHLIGHT_TAG);
		return text -> {
			StringBuilder stringBuilder = new StringBuilder(text);
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				String lang = matcher.group(0);
				String innerContent = matcher.group(1);
				stringBuilder.delete(matcher.start(), matcher.end())
						.insert(matcher.start(),
								CODE_BLOCK_BREAKERS + lang
										+ BREAK
										+ innerContent
										+ BREAK +
										CODE_BLOCK_BREAKERS);
			}
			return Promise.of(stringBuilder.toString());
		};
	}
}
