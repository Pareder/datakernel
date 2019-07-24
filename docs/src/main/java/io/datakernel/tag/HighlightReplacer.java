package io.datakernel.tag;

import io.datakernel.dao.ResourceDao;
import io.datakernel.util.Preconditions;
import org.python.util.PythonInterpreter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.datakernel.util.CollectionUtils.map;
import static java.lang.String.format;
import static java.util.regex.Pattern.DOTALL;

public class HighlightReplacer implements TagReplacer {
	private static final String HIGHLIGHT_TAG = "\\{%\\s+highlight\\s+(\\w+)\\s+%}(.*?)\\{%\\s+endhighlight\\s+%}";
	private static final Map<String, String> SUPPORT_LANGUAGES = map(
			"java", "JavaLexer",
			"xml", "XmlLexer",
			"bash", "BashLexer");
	private static final String DEFAULT_LEXER = SUPPORT_LANGUAGES.get("java");
	private static final String HIGHLIGHT_COMMAND =
			"from pygments import highlight\n"
					+ "from pygments.lexers import %1$s\n"
					+ "from pygments.formatters import HtmlFormatter\n"
					+ "\nresult = highlight(content, %1$s(), HtmlFormatter())";
	private final PythonInterpreter pythonInterpreter;
	private final Pattern pattern = Pattern.compile(HIGHLIGHT_TAG, DOTALL);

	private HighlightReplacer(PythonInterpreter pythonInterpreter) {
		this.pythonInterpreter = pythonInterpreter;
	}

	public static HighlightReplacer create(PythonInterpreter pythonInterpreter) {
		return new HighlightReplacer(pythonInterpreter);
	}

	@Override
	public void replace(StringBuilder text) {
		Matcher matcher = pattern.matcher(text.toString());
		int offset = 0;
		while (matcher.find()) {
			String lang = matcher.group(1);
			String innerContent = matcher.group(2);
			pythonInterpreter.set("content", innerContent);
			pythonInterpreter.exec(format(HIGHLIGHT_COMMAND, SUPPORT_LANGUAGES.getOrDefault(lang, DEFAULT_LEXER)));
			innerContent = pythonInterpreter.get("result", String.class);
			text.replace(matcher.start() + offset, matcher.end() + offset, innerContent);
			offset += innerContent.length() - (matcher.end() - matcher.start());
		}
	}
}
