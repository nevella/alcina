package cc.alcina.framework.servlet.task;

import java.io.FileInputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskRemovePathsFromJar extends PerformerTask {
	private String jarPath;

	private String removeSegmentRegex;

	private boolean dryRun = true;

	private boolean list = false;

	public String getJarPath() {
		return this.jarPath;
	}

	public String getRemoveSegmentRegex() {
		return this.removeSegmentRegex;
	}

	public boolean isDryRun() {
		return this.dryRun;
	}

	public boolean isList() {
		return this.list;
	}

	@Override
	public void run() throws Exception {
		JarWrapper jar = new JarWrapper(jarPath);
		jar.traverse();
		jar.delete();
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public void setJarPath(String jarPath) {
		this.jarPath = jarPath;
	}

	public void setList(boolean list) {
		this.list = list;
	}

	public void setRemoveSegmentRegex(String removeSegmentRegex) {
		this.removeSegmentRegex = removeSegmentRegex;
	}

	class JarWrapper {
		private String path;

		Set<String> paths = new TreeSet<>();

		Set<String> matched = new TreeSet<>();

		JarWrapper(String path) {
			this.path = path;
		}

		void delete() {
			Ax.out("jar path: %s", path);
			if (dryRun) {
				matched.forEach(p -> Ax.out("   %s", p));
			} else {
				matched.forEach(
						p -> Shell.exec("/usr/bin/zip -d %s '%s'", path, p));
			}
			Ax.out("");
		}

		void traverse() throws Exception {
			try (ZipInputStream stream = new ZipInputStream(
					new FileInputStream(path))) {
				ZipEntry entry;
				while ((entry = stream.getNextEntry()) != null) {
					if (entry.isDirectory()) {
					} else {
						paths.add(entry.getName());
					}
				}
			}
			if (list) {
				Ax.out(paths);
			}
			paths.stream().filter(path -> path.matches(removeSegmentRegex))
					.forEach(matched::add);
		}
	}
}
