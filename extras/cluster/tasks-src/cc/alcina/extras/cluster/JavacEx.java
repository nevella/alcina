package cc.alcina.extras.cluster;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;

public class JavacEx extends Javac {
	private Vector excludes = new Vector();

	public void addFileset(FileSet fileset) {
		excludes.add(fileset);
	}

	protected void compile() {
		final FileSet files = (FileSet) excludes.get(0);
		File root = files.getDir(getProject());
		Iterator itr = files.iterator();
		Map<String, File> nameLookup = Arrays.asList(compileList).stream()
				.collect(Collectors.toMap(f -> f.getPath(), f -> f));
		for (; itr.hasNext();) {
			FileResource resource = (FileResource) itr.next();
			nameLookup.remove(resource.getFile().getPath());
		}
		ArrayList<File> list2 = new ArrayList<File>(nameLookup.values());
		compileList=(File[]) list2.toArray(new File[list2.size()]);
		super.compile();
	}
}
