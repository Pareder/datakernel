package datakernel.model;

public final class Doc {
	private final String title;
	private final String path;
	private final String destPath;

	public Doc(String title, String path, String destPath) {
		this.title = title;
		this.path = path;
		this.destPath = destPath;
	}

	public String getDocTitle() {
		return title;
	}

	public String getDocPath() {
		return path;
	}

	public String getDestPath() {
		return destPath;
	}
}
