package cc.alcina.appcreator;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;

public class RenameStartsWith extends MatchingTask {
	private File dir;

	private String fromStartsWith;

	private String toStartsWith;

	@Override
	public void execute() throws BuildException {
		DirectoryScanner ds = super.getDirectoryScanner(dir);
		String[] files = ds.getIncludedFiles();
		for (int i = 0; i < files.length; i++) {
			processFile(files[i]);
		}
	}

	public void setDir(File dir) {
		this.dir = dir;
	}

	public void setFromStartsWith(String fromStartsWith) {
		this.fromStartsWith = fromStartsWith;
	}

	public void setToStartsWith(String toStartsWith) {
		this.toStartsWith = toStartsWith;
	}

	private void processFile(String path) {
		File file = new File(dir, path);
		String name = file.getName();
		if (name.startsWith(fromStartsWith)) {
			file.renameTo(new File(file.getParent() + File.separator
					+ toStartsWith + name.substring(fromStartsWith.length())));
		}
	}
}
