package io.datakernel.automation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class PageParagraphChecker implements PageChecker {
	private static final String LINK_PARAGRAPH_PATTERN = "\\[[\\s\\w-]+?]\\(#([\\w-]+)\\)";
	private static final String PARAGRAPH_PATTERN = "\\#+\\s{1,3}\\**(%s)\\**\\s*\\n";
	private static final String EMPTY = "";
	private final Pattern linkParagraphPattern = Pattern.compile(LINK_PARAGRAPH_PATTERN);

	private PageParagraphChecker() {
	}

	public static PageChecker create() {
		return new PageParagraphChecker();
	}

	@Override
	public Map<String, List<Throwable>> check(StringBuilder content, String fileName) {
		Map<String, List<Throwable>> pathToExceptions = new HashMap<>();

		Matcher match = linkParagraphPattern.matcher(content);
		while (match.find()) {
			String paragraph = match.group(1);
			String patternParagraph = format(PARAGRAPH_PATTERN, paragraph
					.replaceAll("\\s", EMPTY)
					.replaceAll("-", "(.{0,1}\\\\s|\\\\s|\\-)"));
			Pattern pattern = Pattern.compile(patternParagraph, CASE_INSENSITIVE);
			Matcher paragraphMatch = pattern.matcher(content);
			if (!paragraphMatch.find()) {
				List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
				exceptions.add(new PageException(format("Paragraph %s cannot be found", paragraph)));
				pathToExceptions.putIfAbsent(fileName, exceptions);
			}
		}
		return pathToExceptions;
	}
}
