package io.datakernel.automation;

import java.util.List;
import java.util.Map;

public interface PageChecker {
	Map<String, List<Throwable>> check(StringBuilder content, String fileName);
}
