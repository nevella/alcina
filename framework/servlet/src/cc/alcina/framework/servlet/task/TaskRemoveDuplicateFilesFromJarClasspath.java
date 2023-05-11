package cc.alcina.framework.servlet.task;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskRemoveDuplicateFilesFromJarClasspath extends PerformerTask {
	private List<String> jarPaths = new ArrayList<>();

	private boolean dryRun = true;

	private transient Set<String> existingRelativePaths = new TreeSet<>();

	public List<String> getJarPaths() {
		return this.jarPaths;
	}

	public boolean isDryRun() {
		return this.dryRun;
	}

	@Override
	public void run() throws Exception {
		for (String path : jarPaths) {
			JarWrapper jar = new JarWrapper(path);
			jar.traverse();
			jar.deleteDuplicates();
		}
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public void setJarPaths(List<String> jarPaths) {
		this.jarPaths = jarPaths;
	}

	class JarWrapper {
		private String path;

		Set<String> paths = new TreeSet<>();

		Set<String> duplicates = new TreeSet<>();

		JarWrapper(String path) {
			this.path = path;
		}

		void deleteDuplicates() {
			Ax.out("jar path: %s", path);
			if (dryRun) {
				duplicates.forEach(p -> Ax.out("   %s", p));
			} else {
				duplicates.forEach(
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
			paths.stream().filter(path -> !existingRelativePaths.add(path))
					.forEach(duplicates::add);
		}
	}
}
