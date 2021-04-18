package cc.alcina.framework.common.client.csobjects.view;

public class TreePath {
	public static TreePath from(String path) {
		TreePath treePath = new TreePath();
		treePath.setPath(path);
		return treePath;
	}

	private String path;

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}