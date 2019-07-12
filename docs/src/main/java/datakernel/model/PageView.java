package datakernel.model;

import java.util.*;

public final class PageView {
    private final Map<String, List<Doc>> destinationToDocs = new HashMap<>();
    private String path;
    private String content;


    public PageView() {
    }

    public PageView setContent(String content) {
        this.content = content;
        return this;
    }

    public String getPagePath() {
        return path;
    }

    public PageView setPath(String path) {
        this.path = path;
        return this;
    }

    public Set<Map.Entry<String, List<Doc>>> getDestinationToDocs() {
        return destinationToDocs.entrySet();
    }

    public String getPageContent() {
        return content;
    }

    public void put(String docTitle, String docPath, String destination) {
        if (docTitle == null || docPath == null || destination == null) return;
        destinationToDocs.computeIfAbsent(destination, $ -> new ArrayList<>())
                .add(new Doc(docTitle, docPath));
    }
}
