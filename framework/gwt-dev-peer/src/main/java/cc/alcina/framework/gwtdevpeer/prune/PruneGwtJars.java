package cc.alcina.framework.gwtdevpeer.prune;

import java.io.File;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.entity.util.ZipUtil;

/**
 * Prunes the gwt -user and -dev jars, removing classes for which source exists
 * in the gwt-user-peer and gwt-dev-peer mvn projects
 * 
 * Original jars built from https://github.com/gwtproject/gwt :: 8188fd9f8
 * 
 * Note - the code must delete .java files as well as .class (otherwise they'll
 * impact gwt compilation)
 */
public class PruneGwtJars {
	class JarPruneTask {
		String inputJarPath;

		String outputPath;

		String inputSourcePath;

		File staging;

		File inputJarExtracted;

		Set<String> relativeClassfilePaths;

		Set<String> relativeSourcePaths;

		Set<String> overriddenClassfilePaths;

		boolean useZipDeletion;

		String compiledGwtJarPath;

		void run() throws Exception {
			Io.read().path(compiledGwtJarPath).write().toPath(inputJarPath);
			staging = new File("/tmp/PruneGwtJars/staging");
			staging.mkdirs();
			SEUtilities.deleteDirectory(staging);
			staging.mkdirs();
			inputJarExtracted = FileUtils.child(staging, "inputJarExtracted");
			inputJarExtracted.mkdirs();
			ZipUtil.unzip(inputJarExtracted,
					Io.read().path(inputJarPath).asInputStream());
			String extractedPath = inputJarExtracted.getPath() + "/";
			String inputSourcePathSlash = inputSourcePath + "/";
			relativeClassfilePaths = SEUtilities
					.listFilesRecursive(extractedPath, null).stream()
					.filter(file -> file.getName().endsWith(".class"))
					.map(file -> file.getPath().replace(extractedPath, ""))
					.collect(AlcinaCollectors.toLinkedHashSet());
			relativeSourcePaths = SEUtilities
					.listFilesRecursive(inputSourcePathSlash, null).stream()
					.filter(file -> file.getName().endsWith(".java"))
					.map(file -> file.getPath().replace(inputSourcePathSlash,
							""))
					.collect(AlcinaCollectors.toLinkedHashSet());
			overriddenClassfilePaths = relativeClassfilePaths.stream()
					.filter(classfilePath -> {
						return relativeSourcePaths
								.contains(toSourcePath(classfilePath));
					}).collect(AlcinaCollectors.toLinkedHashSet());
			File outputFile = new File(outputPath);
			outputFile.delete();
			if (useZipDeletion) {
				Io.read().path(inputJarPath).write().toFile(outputFile);
				relativeSourcePaths.forEach(path -> {
					String script = "zip -d  '%s' '%s' ";
						try {
							Shell.exec(script, outputPath, path);
						} catch (Exception e) {
							Ax.err("not deleted - %s", path);
						}
				});
				overriddenClassfilePaths.forEach(path -> {
					String script = "zip -d  '%s' '%s' ";
					Shell.exec(script, outputPath, path);
				});
			} else {
				overriddenClassfilePaths.forEach(
						path -> new File(extractedPath + path).delete());
				relativeSourcePaths.forEach(
						path -> new File(extractedPath + path).delete());
				String script = "cd '%s';zip -r  '%s' * ";
				Shell.exec(script, extractedPath, outputPath);
			}
			Ax.sysLogHigh("pruned %s ->\n\t%s", extractedPath, outputPath);
		}

		private String toSourcePath(String classfilePath) {
			String sourcePath = classfilePath
					.replaceFirst("(.+?)(\\$.+?)?\\.class", "$1.java");
			return sourcePath;
		}
	}

	public void run() {
		try {
			run0();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	void run0() throws Exception {
		{
			JarPruneTask userTask = new JarPruneTask();
			userTask.compiledGwtJarPath = "/private/var/local/git/gwt/build/lib/gwt-user.jar";
			userTask.inputJarPath = "/private/var/local/git/alcina/framework/gwt-user-peer/lib/original/gwt-user-2.9.0.f60f5619e.jar";
			userTask.inputSourcePath = "/private/var/local/git/alcina/framework/gwt-user-peer/src/main/java";
			userTask.outputPath = "/private/var/local/git/alcina/framework/gwt-user-peer/lib/pruned/gwt-user-2.9.0-pruned.jar";
			userTask.useZipDeletion = true;
			userTask.run();
		}
		{
			JarPruneTask devTask = new JarPruneTask();
			devTask.compiledGwtJarPath = "/private/var/local/git/gwt/build/lib/gwt-dev.jar";
			devTask.inputJarPath = "/private/var/local/git/alcina/framework/gwt-dev-peer/lib/original/gwt-dev-2.9.0.f60f5619e.jar";
			devTask.inputSourcePath = "/private/var/local/git/alcina/framework/gwt-dev-peer/src/main/java";
			devTask.outputPath = "/private/var/local/git/alcina/framework/gwt-dev-peer/lib/pruned/gwt-dev-2.9.0-pruned.jar";
			devTask.useZipDeletion = true;
			devTask.run();
		}
		{
			CheckNewestFileTask checkTask = new CheckNewestFileTask();
			checkTask.inputJarPath = "/g/alcina/lib/framework/gwt/gwt-dev.jar";
			// checkTask.run();
		}
	}

	class CheckNewestFileTask {
		String inputJarPath;

		File staging;

		File inputJarExtracted;

		void run() {
			try {
				run0();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		void run0() throws Exception {
			staging = new File("/tmp/PruneGwtJars/staging");
			staging.mkdirs();
			SEUtilities.deleteDirectory(staging);
			staging.mkdirs();
			inputJarExtracted = FileUtils.child(staging, "inputJarExtracted");
			inputJarExtracted.mkdirs();
			ZipUtil.unzip(inputJarExtracted,
					Io.read().path(inputJarPath).asInputStream());
			String extractedPath = inputJarExtracted.getPath() + "/";
			List<File> javaFiles = SEUtilities
					.listFilesRecursive(extractedPath, null).stream()
					.filter(f -> f.getName().endsWith(".java")).toList();
			File newest = javaFiles.stream()
					.sorted(Comparator.comparing(File::lastModified).reversed())
					.findFirst().get();
			Ax.out(newest);
			Ax.out(new Date(newest.lastModified()));
		}
	}
}
