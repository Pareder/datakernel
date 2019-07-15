package datakernel.model;

import java.util.*;

public final class PageView {
    private final Map<String, List<Doc>> destinationToDocs = new HashMap<>();
    private String pagePath;
    private String content;

	public PageView(String pagePath, String content) {
		this.pagePath = pagePath;
		this.content = content;
	}

	public PageView() {
	}

	public PageView setContent(String content) {
        this.content = content;
        return this;
    }

    public String getPagePath() {
        return pagePath;
    }

    public PageView setPagePath(String pagePath) {
        this.pagePath = pagePath;
        return this;
    }

    public Set<Map.Entry<String, List<Doc>>> getDestinationToDocs() {
        return destinationToDocs.entrySet();
    }

    public String getPageContent() {
        return content;
    }

    public void put(String docTitle, String docPath, String destPath, String destTitle) {
        if (docTitle == null || docPath == null || destPath == null || destTitle == null) return;
        destinationToDocs.computeIfAbsent(destTitle, $ -> new ArrayList<>())
                .add(new Doc(docTitle, docPath, destPath));
    }
}
