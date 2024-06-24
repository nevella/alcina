package cc.alcina.extras.dev.codeservice;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;

/*
 * Allows code event handlers access to source files in a package, etc
 */
public class SourceFolder implements Predicate<File> {
	public String sourceFolderPath;

	public String sourceFolderCanonicalPath;

	public SourceFolder(String sourceFolderPath) {
		this.sourceFolderPath = sourceFolderPath;
		try {
			this.sourceFolderCanonicalPath = new File(sourceFolderPath)
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

	public class SourcePackage {
		String packageName;

		File file;

		public SourcePackage(File file) {
			this.file = file;
			this.packageName = getPackageName(file);
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
			return Arrays.stream(file.listFiles()).filter(SourceFolder.this)
					.toList();
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
		return new SourcePackage(file);
	}
}
