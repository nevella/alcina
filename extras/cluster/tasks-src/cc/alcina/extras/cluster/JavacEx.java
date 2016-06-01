package cc.alcina.extras.cluster;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;

public class JavacEx extends Javac {
	private List<FileSet> excludes = new ArrayList();

	private Map<String, File> nameLookup;

	public void addFileset(FileSet fileset) {
		excludes.add(fileset);
	}

	protected void compile() {
		nameLookup = Arrays.asList(compileList).stream()
				.collect(Collectors.toMap(f -> f.getPath(), f -> f));
		for (FileSet files : excludes) {
			Iterator itr = files.iterator();
			for (; itr.hasNext();) {
				FileResource resource = (FileResource) itr.next();
				String path = resource.getFile().getPath();
				maybeRemove(path);
				maybeRemove(path.replace("/var/local", "/private/var/local"));
			}
		}
		ArrayList<File> list2 = new ArrayList<File>(nameLookup.values());
		compileList = (File[]) list2.toArray(new File[list2.size()]);
		super.compile();
	}

	private void maybeRemove(String path) {
		boolean debug = Boolean.getBoolean("JavacEx.debug");
		if (nameLookup.containsKey(path)) {
			if (debug) {
				System.out.println("Removed duplicate: " + path);
			}
			nameLookup.remove(path);
		} else {
			if (debug) {
				System.out.println("Not removed: " + path);
			}
		}
	}
}
