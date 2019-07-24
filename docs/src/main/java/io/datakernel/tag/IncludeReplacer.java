package io.datakernel.tag;

import io.datakernel.dao.ResourceDao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

@SuppressWarnings("Duplicates")
public class IncludeReplacer implements TagReplacer {
	public static final String INCLUDE_TAG = "\\{%\\s+include\\s+([\\w-]+\\.\\w+)\\s+([\\w-]+=\".*?\")*\\s*%}";
	public static final String PARAMS = "\\s*([\\w-]+)=\"(.*?)\"";
	public static final String INCLUDE_CONTENT = "\\{\\{\\s+include\\.(.*?)\\s+}}";
	private final Pattern pagePattern = Pattern.compile(INCLUDE_TAG, DOTALL);
	private final Pattern contentPattern = Pattern.compile(INCLUDE_CONTENT, DOTALL);
	private final Pattern paramsPattern = Pattern.compile(PARAMS, DOTALL);
	private final ResourceDao resourceDao;

	private IncludeReplacer(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
	}

	public static IncludeReplacer create(ResourceDao resourceDao) {
		return new IncludeReplacer(resourceDao);
	}

	@Override
	public void replace(StringBuilder text) throws ReplaceException {
		try {
			Matcher matcher = pagePattern.matcher(text.toString());
			Map<String, Params> pathToParams = getPageParams(matcher);
			Map<String, String> pathToContents = new HashMap<>();
			for (Map.Entry<String, Params> entry : pathToParams.entrySet()) {
				String path = entry.getKey();
				pathToContents.put(path, getContent(path, entry.getValue()));
			}

			matcher.reset();
			int offset = 0;
			while (matcher.find()) {
				String content = pathToContents.get(matcher.group(1));
				text.replace(matcher.start() + offset, matcher.end() + offset, content);
				offset += content.length() - (matcher.end() - matcher.start());
			}
		} catch (IOException e) {
			throw new ReplaceException(e);
		}
	}

	private String getContent(String resourceName, Params params) throws IOException {
		String resource = resourceDao.getResource(resourceName);
		StringBuilder content = new StringBuilder(resource);

		Matcher contentMatch = contentPattern.matcher(content);
		int offset = 0;
		while (contentMatch.find()) {
			String key = contentMatch.group(1);
			String value = params.getValue(key);
			if (value == null) {
				continue;
			}
			content.replace(contentMatch.start() + offset, contentMatch.end() + offset, value);
			offset += value.length() - (contentMatch.end() - contentMatch.start());
		}
		return content.toString();
	}

	private Map<String, Params> getPageParams(Matcher matcher) {
		Map<String, Params> pathToParams = new HashMap<>();
		while (matcher.find()) {
			String path = matcher.group(1);
			String includeParams = matcher.group(2);
			Params params = pathToParams.getOrDefault(path, new Params());
			pathToParams.putIfAbsent(path, params);
			if (includeParams == null) {
				return pathToParams;
			}
			Matcher paramsMatch = paramsPattern.matcher(includeParams);
			while (paramsMatch.find()) {
				params.putValue(paramsMatch.group(1), paramsMatch.group(2));
			}
		}
		return pathToParams;
	}

	private static class Params {
		private final Map<String, String> keyValues = new HashMap<>();

		void putValue(String key, String value) {
			keyValues.put(key, value);
		}

		String getValue(String key) {
			return keyValues.get(key);
		}
	}
}
