package io.datakernel.model;

@SuppressWarnings("unused")
public final class Doc {
	private final String title;
	private final String path;

	public Doc(String title, String docFilename) {
		this.title = title;
		this.path = docFilename;
	}

	public String getDocTitle() {
		return title;
	}

	public String getDocPath() {
		return path;
	}
}
