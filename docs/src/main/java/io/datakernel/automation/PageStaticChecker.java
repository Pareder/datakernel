package io.datakernel.automation;

import io.datakernel.async.Promise;
import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.loader.StaticLoader;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.String.format;

public class PageStaticChecker implements PageChecker {
	private static final String STATIC_PATH_PATTERN = "/static(/[\\w-]*)*/[\\w-]+\\.\\w+";
	private static final String ROOT = "/";
	private final Pattern staticPathPattern = Pattern.compile(STATIC_PATH_PATTERN);
	private final ClassLoader classLoader;

	public PageStaticChecker(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public static PageChecker create(ClassLoader classLoader) {
		return new PageStaticChecker(classLoader);
	}

	public static PageChecker create() {
		return new PageStaticChecker(getSystemClassLoader());
	}

	@Override
	public Map<String, List<Throwable>> check(StringBuilder content, String fileName) {
		Map<String, List<Throwable>> pathToExceptions = new HashMap<>();
		Matcher match = staticPathPattern.matcher(content);
		while (match.find()) {
			String resourcePath = match.group();
			int begin = 0;
			if (resourcePath.startsWith(ROOT)) {
				begin++;
			}
			URL resource = classLoader.getResource(resourcePath.substring(begin));
			if (resource == null) {
				List<Throwable> exceptions = pathToExceptions.getOrDefault(fileName, new ArrayList<>());
				exceptions.add(new PageException(format("Cannot find static resource: %s", resourcePath)));
				pathToExceptions.putIfAbsent(fileName, exceptions);
			}
		}
		return pathToExceptions;
	}
}
