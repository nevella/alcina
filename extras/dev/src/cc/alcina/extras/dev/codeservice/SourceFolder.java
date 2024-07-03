package cc.alcina.extras.dev.codeservice;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

/*
 * Allows code event handlers access to source files in a package, etc
 */
public class SourceFolder implements Predicate<File> {
	public String sourceFolderPath;

	public String sourceFolderCanonicalPath;

	public String classPathFolderPath;

	public String classPathFolderCanonicalPath;

	public boolean containsPath(String canonicalPath) {
		return canonicalPath.startsWith(sourceFolderCanonicalPath)
				|| canonicalPath.startsWith(classPathFolderCanonicalPath);
	}

	public SourceFolder(String sourceFolderPath, String classPathFolderPath) {
		this.sourceFolderPath = sourceFolderPath;
		this.classPathFolderPath = classPathFolderPath;
		try {
			this.sourceFolderCanonicalPath = new File(sourceFolderPath)
					.getCanonicalPath();
			this.classPathFolderCanonicalPath = new File(classPathFolderPath)
					.getCanonicalPath();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	@Override
	public String toString() {
		return sourceFolderPath;
	}

	@Override
	public boolean test(File t) {
		return t.isFile() && t.getName().matches("[A-Za-z0-9_]+.java");
	}

	public String getPackageName(File file) {
		try {
			String canonicalPath = file.getCanonicalPath();
			Preconditions.checkState(
					canonicalPath.startsWith(sourceFolderCanonicalPath));
			String relativePath = canonicalPath
					.substring(sourceFolderCanonicalPath.length());
			if (relativePath.startsWith("/")) {
				relativePath = relativePath.substring(1);
			}
			return relativePath.replace("/", ".");
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public static class SourcePackage {
		String packageName;

		File file;

		SourceFolder sourceFolder;

		public SourcePackage(SourceFolder sourceFolder, File file) {
			this.sourceFolder = sourceFolder;
			this.file = file;
			this.packageName = sourceFolder.getPackageName(file);
		}

		public SourcePackage(String packageName) {
			this.packageName = packageName;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SourcePackage) {
				return ((SourcePackage) obj).packageName.equals(packageName);
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return packageName.hashCode();
		}

		public List<File> listFiles() {
			return Arrays.stream(file.listFiles()).filter(sourceFolder)
					.collect(Collectors.toList());
		}

		@Override
		public String toString() {
			return packageName;
		}
	}

	public class SourceFile {
		File file;

		public SourceFile(File file) {
			this.file = file;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SourceFile) {
				return ((SourceFile) obj).file.equals(file);
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return file.hashCode();
		}
	}

	public SourceFile sourceFile(File file) {
		return new SourceFile(file);
	}

	public SourcePackage sourcePackage(File file) {
		if (file.isFile()) {
			file = file.getParentFile();
		}
		return new SourcePackage(this, file);
	}

	public boolean testGeneratePackageEvent(File file) {
		return !file.getName().equals("PackageProperties.java");
	}

	public File translateClassPathFileToSourceFile(File file) {
		try {
			String canonicalPath = file.getCanonicalPath();
			if (canonicalPath.startsWith(classPathFolderCanonicalPath)
					&& canonicalPath.endsWith(".class")) {
				String path = sourceFolderCanonicalPath + canonicalPath
						.substring(classPathFolderCanonicalPath.length());
				String folder = path.replaceFirst("(.+)/(.+)", "$1");
				String classFileName = path.replaceFirst("(.+)/(.+)", "$2");
				String sourceName = classFileName
						.replaceFirst("(.+?)(\\$.+)?(\\.class)", "$1.java");
				return new File(Ax.format("%s/%s", folder, sourceName));
			} else {
				return file;
			}
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}
