package cc.alcina.appcreator;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class PruneEmpty extends Task {
	private File dir;
	public void setDir(File dir) {
		this.dir = dir;
	}
	@Override
	public void execute() throws BuildException {
		PackageUtils.prune(dir);
	}
}
