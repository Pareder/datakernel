package datakernel.tag;

import datakernel.dao.ResourceDao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

public class IncludeReplacer implements TagReplacer {
	private static final String INCLUDE_TAG = "\\{%\\s+include\\s+(.+\\.\\w+)\\s+(.*?)?\\s*%}";
	private static final String PARAMS = "\\s*(\\w+)=(['\"`].*['\"`])";
	private static final String INCLUDE_CONTENT = "\\{\\{\\s+include\\.(.+)\\s+}}";
	private final Pattern pagePattern = Pattern.compile(INCLUDE_TAG, DOTALL);
	private final Pattern contentPattern = Pattern.compile(INCLUDE_CONTENT, DOTALL);
	private final Pattern paramsPattern = Pattern.compile(PARAMS, DOTALL);
	private final ResourceDao resourceDao;

	public IncludeReplacer(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
	}

	@Override
	public void replace(StringBuilder text) throws ReplaceException {
		try {
			Matcher matcher = pagePattern.matcher(text.toString());
			Map<String, Params> pathToParams = getPageParams(matcher);
			for (Map.Entry<String, Params> entry : pathToParams.entrySet()) {
				String path = entry.getKey();
				String content = getContent(path, pathToParams.get(path));

				matcher.reset();
				int offset = 0;
				while (matcher.find()) {
					String foundPath = matcher.group(1);
					if (foundPath.equals(path)) {
						text.replace(matcher.start() + offset, matcher.end() + offset, content);
						offset += content.length() - (matcher.end() - matcher.start());
					}
				}
			}
		} catch (IOException e) {
			throw new ReplaceException(e);
		}
	}

	private String getContent(String resourceName, Params params) throws ReplaceException, IOException {
		String resourcePath = resourceDao.getResource(resourceName);
		StringBuilder content = new StringBuilder(resourcePath);

		Matcher contentMatch = contentPattern.matcher(content);
		while (contentMatch.find()) {
			String key = contentMatch.group(1);
			String value = params.getValue(key);
			if (value == null) {
				throw new ReplaceException("Key :" + key + " cannot be found for " + resourceName);
			}
			content.replace(contentMatch.start(), contentMatch.end(), value);
		}
		return content.toString();
	}

	private Map<String, Params> getPageParams(Matcher matcher) {
		Map<String, Params> pathToParams = new HashMap<>();
		while (matcher.find()) {
			String path = matcher.group(1);
			Params params = pathToParams.getOrDefault(path, new Params());
			pathToParams.putIfAbsent(path, params);
			Matcher paramsMatch = paramsPattern.matcher(matcher.group(2));
			while (paramsMatch.find()) {
				String key = paramsMatch.group(1);
				String value = paramsMatch.group(2);
				params.putValue(key, value);
			}
		}
		return pathToParams;
	}

	private class Params {
		private final Map<String, String> keyValues = new HashMap<>();

		void putValue(String key, String value) {
			keyValues.put(key, value);
		}

		String getValue(String key) {
			return keyValues.get(key);
		}
	}
}
