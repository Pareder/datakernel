package io.datakernel.automation;

import io.datakernel.dao.ResourceDao;
import io.datakernel.tag.ReplaceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.datakernel.tag.IncludeReplacer.*;
import static java.util.regex.Pattern.DOTALL;

@SuppressWarnings("Duplicates")
public class PageIncludeChecker implements PageChecker {
	private final Pattern pagePattern = Pattern.compile(INCLUDE_TAG, DOTALL);
	private final Pattern contentPattern = Pattern.compile(INCLUDE_CONTENT, DOTALL);
	private final Pattern paramsPattern = Pattern.compile(PARAMS, DOTALL);
	private final ResourceDao resourceDao;

	private PageIncludeChecker(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
	}

	public static PageIncludeChecker create(ResourceDao resourceDao) {
		return new PageIncludeChecker(resourceDao);
	}

	@Override
	public Map<String, List<Throwable>> check(StringBuilder content, String fileName) {
		Map<String, List<Throwable>> pathToExceptions = new HashMap<>();
		Map<String, Map<String, String>> pageParams = getPageParams(content.toString());
		for (Map.Entry<String, Map<String, String>> entry : pageParams.entrySet()) {
			try {
				String path = entry.getKey();
				String resource = resourceDao.getResource(path);

				Matcher contentMatch = contentPattern.matcher(resource);
				while (contentMatch.find()) {
					String key = contentMatch.group(1);
					String value = entry.getValue().get(key);
					if (value == null) {
						List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
						exceptions.add(new PageException("Key: " + key + ": cannot be found in match: " + contentMatch.group()));
						pathToExceptions.putIfAbsent(fileName, exceptions);
					}
				}
			} catch (IOException e) {
				List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
				exceptions.add(e);
				pathToExceptions.putIfAbsent(fileName, exceptions);
			}
		}

		return pathToExceptions;
	}

	private Map<String, Map<String, String>> getPageParams(String content) {
		Matcher matcher = pagePattern.matcher(content);
		Map<String, Map<String, String>> pathToParams = new HashMap<>();
		while (matcher.find()) {
			String path = matcher.group(1);
			String includeParams = matcher.group(2);
			Map<String, String> params = pathToParams.getOrDefault(path, new HashMap<>());
			pathToParams.putIfAbsent(path, params);
			if (includeParams == null) {
				return pathToParams;
			}
			Matcher paramsMatch = paramsPattern.matcher(includeParams);
			while (paramsMatch.find()) {
				params.put(paramsMatch.group(1), paramsMatch.group(2));
			}
		}
		return pathToParams;
	}
}
