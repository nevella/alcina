/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;

/**
 *
 * @author Nick Reddel
 */
public class ClasspathScanner {
	public static final String CONTEXT_EXTRA_CLASSLOADERS = ClasspathScanner.class
			+ ".CONTEXT_EXTRA_CLASSLOADERS";

	protected static List<Class<? extends ClasspathVisitor>> visitors = new ArrayList<Class<? extends ClasspathVisitor>>();
	static {
		visitors.add(DirectoryVisitorNio.class);
		visitors.add(JarVisitor.class);
	}

	public static List<ClassLoader> getScannerClassLoadersToTry() {
		List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
		classLoaders.add(Thread.currentThread().getContextClassLoader());
		List<ClassLoader> extraClassLoaders = LooseContext
				.get(CONTEXT_EXTRA_CLASSLOADERS);
		if (extraClassLoaders != null) {
			classLoaders.addAll(extraClassLoaders);
		}
		return classLoaders;
	}

	public static void
			installVisitor(Class<? extends ClasspathVisitor> visitor) {
		visitors.add(visitor);
	};

	private String pkg;

	private boolean recur = false;

	private boolean ignoreJars;

	public ClassMetadataCache classDataCache = new ClassMetadataCache();

	public ClasspathScanner(String pkg) {
		this(pkg, false, false);
	}

	public ClasspathScanner(String pkg, boolean subpackages,
			boolean ignoreJars) {
		recur = subpackages;
		this.ignoreJars = ignoreJars;
		sanitizePackage(pkg);
	}

	public ClassMetadataCache getClassDataCache() {
		return this.classDataCache;
	}

	public ClassMetadataCache getClasses() throws Exception {
		getClassNames();
		return classDataCache;
	}

	public Set<String> getClassNames() throws IOException {
		String[] cpElts = System.getProperty("java.class.path").split(":");
		for (String pathElt : cpElts) {
			String path = pathElt;
			if ((path != null) && (path.trim().length() > 0)) {
				URL url = new URL("file", null, path);
				invokeHandler(url);
			}
		}
		return classDataCache.classData.keySet();
	}

	public String getPkg() {
		return pkg;
	}

	public void invokeHandler(URL url) {
		try {
			for (Class<? extends ClasspathVisitor> visitorClass : visitors) {
				ClasspathVisitor visitor = visitorClass
						.getConstructor(ClasspathScanner.class)
						.newInstance(this);
				if (visitor.handles(url)) {
					visitor.enumerateClasses(url);
					break;
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public boolean isIgnoreJars() {
		return ignoreJars;
	}

	public boolean isRecur() {
		return recur;
	}

	public void scanDirectory(String path) throws Exception {
		URL url = new File(path).toURI().toURL();
		new DirectoryVisitor(this).enumerateClasses(url);
	}

	private void sanitizePackage(String pkgName) {
		if ((pkgName == null) || (pkgName.trim().length() == 0))
			throw new IllegalArgumentException("Base package cannot be null");
		pkg = pkgName.replace('.', '/');
		if (getPkg().endsWith("*"))
			pkg = getPkg().substring(0, getPkg().length() - 1);
		if (getPkg().endsWith("/"))
			pkg = getPkg().substring(0, getPkg().length() - 1);
	}

	protected String getPackage() {
		return getPkg();
	}

	protected URL invokeResolver(URL url) {
		try {
			for (Class<? extends ClasspathVisitor> visitorClass : visitors) {
				ClasspathVisitor visitor = visitorClass
						.getConstructor(ClasspathScanner.class)
						.newInstance(this);
				URL resolved = visitor.resolve(url);
				if (resolved != null) {
					return resolved;
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return url;
	}

	public abstract static class ClasspathVisitor {
		protected static final Object PROTOCOL_FILE = "file";

		protected final ClasspathScanner scanner;

		public ClasspathVisitor(ClasspathScanner scanner) {
			this.scanner = scanner;
		}

		public abstract void enumerateClasses(URL url) throws Exception;

		public abstract boolean handles(URL url);

		public URL resolve(URL url) throws Exception {
			return null;
		}

		protected synchronized void add(String relativeClassPath,
				long modificationDate, URL url, InputStream inputStream) {
			relativeClassPath = relativeClassPath.replace("\\", "/");
			if ((relativeClassPath.startsWith(scanner.getPkg()))
					&& (relativeClassPath.endsWith(".class"))) {
				boolean add = scanner.isRecur() ? true
						: relativeClassPath
								.substring(scanner.getPkg().length() + 1)
								.indexOf("/") < 0;
				if (add) {
					ClassMetadata item = ClassMetadata.fromRelativeSourcePath(
							relativeClassPath, url, inputStream,
							modificationDate);
					scanner.classDataCache.insert(item);
				}
			}
		}

		protected String sanitizeFileURL(URL url) {
			return sanitizeFileURLForWindows(url.getFile());
		}

		protected String sanitizeFileURLForWindows(String path) {
			path = path.replace("\\", "/");
			String tmp = path;
			if (tmp.indexOf("%20") > 0)
				tmp = tmp.replaceAll("%20", " "); // Encodes
			if ((tmp.indexOf(":") >= 0) && (tmp.startsWith("/")))
				tmp = tmp.substring(1); // Removes leading / in URLs like
			// /c:/...
			return tmp;
		}
	}

	public static class DirectoryVisitor extends ClasspathVisitor {
		public DirectoryVisitor(ClasspathScanner scanner) {
			super(scanner);
		}

		@Override
		public void enumerateClasses(URL url) throws Exception {
			String file = sanitizeFileURL(url);
			getClassesFromDirectory(file, file);
		}

		@Override
		public boolean handles(URL url) {
			return url.getProtocol().equals(PROTOCOL_FILE)
					&& new File(url.getFile()).isDirectory();
		}

		private void getClassesFromDirectory(String path, String root) {
			File directory = new File(path);
			if (directory.exists()) {
				for (String file : directory.list()) {
					File f = new File(directory, file);
					if (f.isFile()) {
						if (f.getPath().endsWith(".class"))
							try {
								add(path.substring(root.length() + 1) + "/"
										+ file, f.lastModified(),
										f.toURI().toURL(), null);
							} catch (Exception e) {
								e.printStackTrace();
							}
					} else if (scanner.isRecur()) {
						getClassesFromDirectory(path + "/" + file, root);
					}
				}
			}
		}
	}

	public static class DirectoryVisitorNio extends ClasspathVisitor {
		public DirectoryVisitorNio(ClasspathScanner scanner) {
			super(scanner);
		}

		@Override
		public void enumerateClasses(URL url) throws Exception {
			String fileUrl = sanitizeFileURL(url);
			Path startingDir = Paths.get(fileUrl);
			ScanFiles scanFiles = new ScanFiles(fileUrl);
			Files.walkFileTree(startingDir, scanFiles);
		}

		@Override
		public boolean handles(URL url) {
			return url.getProtocol().equals(PROTOCOL_FILE)
					&& new File(url.getFile()).isDirectory();
		}

		public class ScanFiles extends SimpleFileVisitor<Path> {
			private String root;

			public ScanFiles(String root) {
				this.root = root;
			}

			@Override
			public FileVisitResult visitFile(Path path,
					BasicFileAttributes attr) {
				try {
					String pathString = path.toString();
					if (attr.isRegularFile() && pathString.endsWith(".class")) {
						String classPath = pathString
								.substring(root.length() + 1);
						String prefix = pathString.startsWith("/") ? "file://"
								: "file:///";
						add(classPath, attr.lastModifiedTime().toMillis(),
								new URL(prefix + pathString), null);
					}
					return FileVisitResult.CONTINUE;
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
	}

	public static class JarVisitor extends ClasspathVisitor {
		public JarVisitor(ClasspathScanner scanner) {
			super(scanner);
		}

		@Override
		public void enumerateClasses(URL url) throws Exception {
			String jarPath = sanitizeFileURL(url);
			JarFile jarFile = new JarFile(jarPath);
			for (Enumeration<JarEntry> e = jarFile.entries(); e
					.hasMoreElements();) {
				JarEntry jarEntry = e.nextElement();
				if (jarEntry != null && jarEntry.getName().endsWith(".class")) {
					URL entryUrl = SEUtilities.toURL(Ax.format("%s!/%s",
							url.toString().replace("file:/", "jar:file:/")
									.replace(" ", "%20"),
							jarEntry.getName()));
					add(jarEntry.getName(), jarEntry.getTime(), entryUrl,
							jarFile.getInputStream(jarEntry));
				}
			}
			jarFile.close();
		}

		@Override
		public boolean handles(URL url) {
			return !scanner.isIgnoreJars()
					&& url.getProtocol().equals(PROTOCOL_FILE)
					&& (url.getFile().endsWith(".jar")
							|| url.getFile().endsWith(".apk"));
		}
	}

	public static class ServletClasspathScanner extends ClasspathScanner {
		protected static URL cleanUrl(String resourceName, URL url)
				throws UnsupportedEncodingException, MalformedURLException {
			String urlPath = url.getFile();
			String protocol = url.getProtocol();
			urlPath = URLDecoder.decode(urlPath, "UTF-8");
			if (urlPath.startsWith("file:")) {
				// On windows urlpath looks like file:/C: on Linux
				// file:/home
				// substring(5) works for both
				urlPath = urlPath.substring(5);
				if (protocol.equals("jar")) {
					protocol = "file";
				}
			}
			if (urlPath.indexOf('!') > 0) {
				urlPath = urlPath.substring(0, urlPath.indexOf('!'));
			} else {
				File dirOrArchive = new File(urlPath);
				if (resourceName != null && resourceName.lastIndexOf('/') > 0) {
					// for META-INF/components.xml
					dirOrArchive = dirOrArchive.getParentFile();
				}
				urlPath = dirOrArchive.getParent();
			}
			if (urlPath.contains("\\")) {
				urlPath = urlPath.replace('\\', '/');
				if (!urlPath.startsWith("/")) {
					urlPath = "/" + urlPath;
				}
			}
			URL newUrl = new URL(protocol, url.getHost(), urlPath);
			return newUrl;
		}

		private final Object logger;

		private final String resourceName;

		protected List<String> ignorePathSegments;

		public ServletClasspathScanner(String pkg, boolean subpackages,
				boolean ignoreJars, Object logger, String resourceName,
				List<String> ignorePathSegments) {
			this(pkg, subpackages, ignoreJars, logger, resourceName,
					ignorePathSegments, new ArrayList<String>());
		}

		public ServletClasspathScanner(String pkg, boolean subpackages,
				boolean ignoreJars, Object logger, String resourceName,
				List<String> ignorePathSegments,
				List<String> ignorePackageSegments) {
			super(pkg, subpackages, ignoreJars);
			this.logger = logger;
			this.resourceName = resourceName;
			this.ignorePathSegments = ignorePathSegments;
			classDataCache.ignorePackageSegments = ignorePackageSegments;
		}

		@Override
		public ClassMetadataCache getClasses() throws Exception {
			Stream<URL> urls = getScannerClassLoadersToTry().stream()
					.flatMap(classLoader -> {
						try {
							List<URL> list1 = scanForRegProps(resourceName,
									classLoader);
							List<URL> list2 = scanForRegProps(
									"META-INF/" + resourceName, classLoader);
							return Stream.concat(list1.stream(), list2.stream())
									.distinct();
						} catch (Exception e) {
							throw new WrappedRuntimeException(e);
						}
					});
			// because this occurs very early, it doesn't seem parallel() helps
			// (since UnixFile.toString() is a big hit, either way)
			//
			// FIXME - console - perf - possibly check with GraalVM/a
			// precompiler?
			urls.collect(Collectors.toList()).stream()
					// .parallel()
					.forEach(url -> invokeHandler(url));
			return classDataCache;
		}

		@SuppressWarnings("unused")
		private void info(String message) {
			if (logger instanceof Logger) {
				((Logger) logger).info(message);
			} else {
				System.out.println(message);
			}
		}

		// lifted from seam 1.21
		private List<URL> scanForRegProps(String resourceName,
				ClassLoader classLoader) throws Exception {
			List<URL> urls = new ArrayList<URL>();
			if (resourceName == null) {
				for (URL url : ((URLClassLoader) classLoader).getURLs()) {
					String urlPath = url.getFile();
					if (urlPath.endsWith("/")) {
						urlPath = urlPath.substring(0, urlPath.length() - 1);
					}
					urls.add(new URL(url.getProtocol(), null, urlPath));
				}
			} else {
				try {
					Enumeration<URL> urlEnum = classLoader
							.getResources(resourceName);
					while (urlEnum.hasMoreElements()) {
						URL url = urlEnum.nextElement();
						url = invokeResolver(url);
						URL newUrl = cleanUrl(resourceName, url);
						urls.add(newUrl);
					}
				} catch (IOException ioe) {
					warn("could not read: " + resourceName, ioe);
					return Collections.emptyList();
				}
			}
			return urls.stream().filter(url -> {
				String urlPath = url.getFile();
				return !ignorePathSegments.stream()
						.anyMatch(s -> urlPath.contains(s));
			}).collect(Collectors.toList());
		}

		private void warn(String message, Exception t) {
			if (logger instanceof Logger) {
				((Logger) logger).warn(message, t);
			} else {
				System.out.println(message);
				t.printStackTrace();
			}
		}
	}
}
