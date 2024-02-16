package cc.alcina.appcreator;

import java.util.ArrayList;
import java.util.List;

public class DownloadableFileList {
	public void add(DownloadableFile file) {
		files.add(file);
	}

	private List<DownloadableFile> files = new ArrayList<DownloadableFile>();

	public void setFiles(List<DownloadableFile> files) {
		this.files = files;
	}

	public List<DownloadableFile> getFiles() {
		return files;
	}
}
