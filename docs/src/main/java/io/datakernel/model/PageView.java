package io.datakernel.model;

import java.util.*;

@SuppressWarnings("unused")
public final class PageView {
    private final Map<String, List<Doc>> destinationToDocs = new HashMap<>();
    private final Map<String, String> properties;
    private final String pagePath;
    private final String content;
	private String renderedContent;

	public PageView(String pagePath, String content, Map<String, String> properties) {
		this.pagePath = pagePath;
		this.content = content;
		this.properties = properties;
	}

    public String getPagePath() {
        return pagePath;
    }

    public Set<Map.Entry<String, List<Doc>>> getDestinationToDocs() {
        return destinationToDocs.entrySet();
    }

	public Map<String, String> getProperties() {
		return properties;
	}

	public String getPageContent() {
        return content;
    }

	public String getRenderedContent() {
		return renderedContent;
	}

	public void putProperty(String key, String value) {
		properties.put(key, value);
	}

	public void putSubParagraph(String docTitle, String docPath, String destTitle, String destPath) {
        if (docTitle == null || docPath == null || destPath == null || destTitle == null) return;
        destinationToDocs.computeIfAbsent(destTitle, $ -> new ArrayList<>())
                .add(new Doc(docTitle, docPath, destPath));
    }

	public PageView setRenderedContent(String renderedContent) {
		this.renderedContent = renderedContent;
		return this;
	}
}
