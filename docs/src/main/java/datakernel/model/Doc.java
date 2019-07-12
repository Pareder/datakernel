package datakernel.model;

public final class Doc {
	private final String title;
	private final String path;

	public Doc(String title, String path) {
		this.title = title;
		this.path = path;
	}

	public String getDocTitle() {
		return title;
	}

	public String getDocPath() {
		return path;
	}
}
