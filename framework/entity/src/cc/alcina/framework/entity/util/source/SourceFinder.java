package cc.alcina.framework.entity.util.source;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;

public interface SourceFinder {
	static List<SourceFinder> sourceFinders = Collections
			.synchronizedList(new ArrayList<>());

	static void ensureDefaultFinders() {
		synchronized (sourceFinders) {
			if (sourceFinders.isEmpty()) {
				sourceFinders.add(new SourceFinderFs());
			}
		}
	}

	static File findSourceFileUnchecked(Class clazz) {
		try {
			return findSourceFile(clazz);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	static File findSourceFile(Class clazz) throws Exception {
		ensureDefaultFinders();
		clazz = clazz.getNestHost();
		for (SourceFinder finder : sourceFinders) {
			File file = finder.findSourceFile0(clazz);
			if (file != null) {
				return file;
			}
		}
		Ax.err("Warn - cannot find source:\n\t%s", clazz.getName());
		return null;
	}

	static String findSource(Class clazz) throws Exception {
		ensureDefaultFinders();
		clazz = clazz.getNestHost();
		for (SourceFinder finder : sourceFinders) {
			String source = finder.findSource0(clazz);
			if (source != null) {
				return source;
			}
		}
		Ax.err("Warn - cannot find source:\n\t%s", clazz.getName());
		return null;
	}

	default String findSource0(Class clazz) {
		File sourceFile = findSourceFile0(clazz);
		return sourceFile == null ? null
				: Io.read().file(sourceFile).asString();
	}

	File findSourceFile0(Class clazz);

	static class SourceFinderFs implements SourceFinder {
		@Override
		public File findSourceFile0(Class clazz) {
			try {
				CodeSource codeSource = clazz.getProtectionDomain()
						.getCodeSource();
				URL classFileLocation = codeSource.getLocation();
				URL sourceFileLocation = new URI(
						Ax.format("%s%s.java", classFileLocation.toString(),
								clazz.getName().replace(".", "/"))).toURL();
				File file = new File(toPath(sourceFileLocation));
				if (file.exists()
						&& !sourceFileLocation.toString().contains("/build/")) {
					return file;
				}
				sourceFileLocation = new URI(sourceFileLocation.toString()
						.replace("/alcina/bin/",
								"/alcina/framework/entity/src/")
						.replace("/bin/", "/src/")
						.replace("/WebRoot/WEB-INF/classes/", "/src/")
						.replace("/build/classes/", "/src/")).toURL();
				if (sourceFileLocation.toString().contains("/build/")
						&& !sourceFileLocation.toString().contains("/src/")) {
					sourceFileLocation = new URI(sourceFileLocation.toString()
							.replace("/build/", "/src/")).toURL();
				}
				File file2 = new File(toPath(sourceFileLocation));
				if (file2.exists()) {
					return file2;
				}
				sourceFileLocation = new URI(sourceFileLocation.toString()
						.replace("/alcina/framework/entity/src/",
								"/alcina/framework/common/src/")).toURL();
				File file3 = new File(toPath(sourceFileLocation));
				if (file3.exists()) {
					return file3;
				}
				sourceFileLocation = new URI(sourceFileLocation.toString()
						.replace("/alcina/framework/common/src/",
								"/alcina/framework/gwt/src/")).toURL();
				File file4 = new File(toPath(sourceFileLocation));
				if (file4.exists()) {
					return file4;
				}
				sourceFileLocation = new URI(sourceFileLocation.toString()
						.replace("/alcina/framework/gwt/src/",
								"/alcina/framework/servlet/src/")).toURL();
				File file5 = new File(toPath(sourceFileLocation));
				if (file5.exists()) {
					return file5;
				}
				Optional<SourceFinderFsHelper> helper = Registry
						.optional(SourceFinderFsHelper.class);
				if (helper.isPresent()) {
					sourceFileLocation = new URI(sourceFileLocation.toString()
							.replace("/alcina/framework/entity/src/",
									"/alcina/framework/common/src/")).toURL();
					File file6 = new File(toPath(sourceFileLocation));
					if (file6.exists()) {
						return file5;
					}
				}
				return null;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private String toPath(URL sourceFileLocation) {
			return sourceFileLocation.toString().replaceFirst("^file:/*/", "/");
		}
	}

	public static interface SourceFinderFsHelper {
		public URL transformPath(URL path);
	}
}