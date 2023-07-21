package cc.alcina.framework.entity.persistence.mvcc;

import java.io.File;
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

	String findSource0(Class clazz);

	static class SourceFinderFs implements SourceFinder {
		@Override
		public String findSource0(Class clazz) {
			try {
				CodeSource codeSource = clazz.getProtectionDomain()
						.getCodeSource();
				URL classFileLocation = codeSource.getLocation();
				URL sourceFileLocation = new URL(
						Ax.format("%s%s.java", classFileLocation.toString(),
								clazz.getName().replace(".", "/")));
				if (new File(toPath(sourceFileLocation)).exists()
						&& !sourceFileLocation.toString().contains("/build/")) {
					return Io.read().url(sourceFileLocation.toString())
							.asString();
				}
				sourceFileLocation = new URL(sourceFileLocation.toString()
						.replace("/alcina/bin/",
								"/alcina/framework/entity/src/")
						.replace("/bin/", "/src/")
						.replace("/build/classes/", "/src/"));
				if (sourceFileLocation.toString().contains("/build/")
						&& !sourceFileLocation.toString().contains("/src/")) {
					sourceFileLocation = new URL(sourceFileLocation.toString()
							.replace("/build/", "/src/"));
				}
				if (new File(toPath(sourceFileLocation)).exists()) {
					return Io.read().url(sourceFileLocation.toString())
							.asString();
				}
				sourceFileLocation = new URL(sourceFileLocation.toString()
						.replace("/alcina/framework/entity/src/",
								"/alcina/framework/common/src/"));
				if (new File(toPath(sourceFileLocation)).exists()) {
					return Io.read().url(sourceFileLocation.toString())
							.asString();
				}
				Optional<SourceFinderFsHelper> helper = Registry
						.optional(SourceFinderFsHelper.class);
				if (helper.isPresent()) {
					sourceFileLocation = new URL(sourceFileLocation.toString()
							.replace("/alcina/framework/entity/src/",
									"/alcina/framework/common/src/"));
					if (new File(toPath(sourceFileLocation)).exists()) {
						return Io.read().url(sourceFileLocation.toString())
								.asString();
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