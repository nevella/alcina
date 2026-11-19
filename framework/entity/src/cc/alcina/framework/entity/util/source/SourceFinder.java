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
				{
					File file = new File(toPath(sourceFileLocation));
					if (file.exists() && !sourceFileLocation.toString()
							.contains("/build/")) {
						return file;
					}
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
				if (sourceFileLocation.getPath()
						.contains("/alcina/framework/entity/src/")) {
					List<String> alcinaRoots = List.of(
							"/alcina/framework/common/src/main/java/",
							"/alcina/framework/gwt-user-peer/src/main/java/",
							"/alcina/framework/gwt-dev-peer/src/main/java/",
							"/alcina/framework/gwt/src/",
							"/alcina/framework/servlet/src/");
					for (String alcinaRoot : alcinaRoots) {
						URL testLocation = new URI(sourceFileLocation.toString()
								.replace("/alcina/framework/entity/src/",
										alcinaRoot)).toURL();
						File file = new File(toPath(testLocation));
						if (file.exists()) {
							return file;
						}
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
}