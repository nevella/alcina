package cc.alcina.framework.classmeta;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.util.ClasspathScanner;

public class ClasspathScannerResolver {
	Map<URL, ListeningCache> caches = new LinkedHashMap<>();

	public synchronized ClassMetadataCache handle(ClassMetaRequest typedRequest,
			boolean debug) {
		// await update queue
		ClassMetadataCache result = new ClassMetadataCache();
		for (URL url : typedRequest.classPaths) {
			if (debug) {
				Ax.out("listening cache: %s", url);
			}
			if (!caches.containsKey(url)) {
				ensureCache(url);
			}
			result.merge(caches.get(url).classes);
		}
		return result;
	}

	public synchronized void refreshJars() {
		caches.values().forEach(ListeningCache::checkUptodateIfJar);
	}

	private void ensureCache(URL url) {
		try {
			ListeningCache cache = new ListeningCache(url);
			cache.reloadClasses();
			caches.put(url, cache);
			cache.startListeners();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	class ListeningCache {
		private URL url;

		private ClassMetadataCache classes;

		long reloadTime;

		private Path listeningPath;

		private boolean jar;

		public ListeningCache(URL url) {
			this.url = url;
		}

		public void refresh(String filePath, String relativeClassPath) {
			synchronized (ClasspathScannerResolver.this) {
				try {
					File file = new File(filePath);
					URL url = file.toURI().toURL();
					ClassMetadata item = ClassMetadata.fromRelativeSourcePath(
							relativeClassPath, url, null, file.lastModified());
					synchronized (ClasspathScannerResolver.this) {
						Ax.out("Classpath scanner delta: %s\n\t%s",
								file.exists() ? "(add/mod)" : "delete",
								item.className);
						classes.classData.remove(item.className);
						if (file.exists()) {
							classes.classData.put(item.className, item);
						}
					}
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}

		public void reloadClasses() {
			ClasspathScanner scanner = new ClasspathScanner("*", true,
					!url.toString().endsWith(".jar"));
			scanner.invokeHandler(url);
			this.classes = scanner.classDataCache;
			updatePathInfo();
		}

		public void startListeners() {
			if (url.getProtocol().equals("file")) {
				addPathListeners();
			}
		}

		private void addPathListeners() {
			new Thread() {
				@Override
				public void run() {
					try {
						switch (SEUtilities.getOsType()) {
						case MacOS:
							// automagically recursive
							new ClasspathWatchDirOsX(listeningPath)
									.processEvents();
							break;
						default:
							new ClasspathWatchDir(listeningPath, true)
									.processEvents();
							break;
						}
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				};
			}.start();
		}

		private void updatePathInfo() {
			if (url.getProtocol().equals("file")) {
				String localPath = url.toString().replaceFirst("file:/+", "/");
				listeningPath = Paths.get(localPath);
				reloadTime = listeningPath.toFile().lastModified();
				jar = url.toString().endsWith(".jar");
			}
		}

		void checkUptodateIfJar() {
			if (jar) {
				if (listeningPath.toFile().lastModified() > reloadTime) {
					Ax.out("Reload from uptodate check");
					String key = Ax.format("reload: %s",
							listeningPath.toFile().getName());
					MetricLogging.get().start(key);
					ListeningCache.this.reloadClasses();
					MetricLogging.get().end(key);
				}
			}
		}

		class ClasspathWatchDir extends WatchDir {
			ClasspathWatchDir(Path dir, boolean recursive) throws IOException {
				super(dir, recursive);
			}

			@Override
			protected void handleEvent(WatchEvent<?> event, Path name,
					Path child) {
				String childPath = child.toString();
				if (childPath.endsWith(".class")) {
					String localPath = url.toString().replaceFirst("file:/+",
							"/");
					String classPath = childPath.substring(localPath.length());
					ListeningCache.this.refresh(childPath, classPath);
				}
			}
		}

		class ClasspathWatchDirOsX extends WatchDirOsX {
			ClasspathWatchDirOsX(Path dir) throws IOException {
				super(dir);
			}

			@Override
			protected void handleEvent(
					com.barbarysoftware.watchservice.WatchEvent<?> event,
					File file) {
				String childPath = file.toString();
				if (childPath.endsWith(".class")) {
					String localPath = url.toString().replaceFirst("file:/+",
							"/");
					String classPath = childPath.substring(localPath.length());
					ListeningCache.this.refresh(childPath, classPath);
				} else if (childPath.endsWith(".jar")) {
					checkUptodateIfJar();
				}
			}
		}
	}
}
