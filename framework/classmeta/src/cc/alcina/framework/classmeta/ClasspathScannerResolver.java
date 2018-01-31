package cc.alcina.framework.classmeta;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.util.ClasspathScanner;

public class ClasspathScannerResolver {
	Map<URL, ListeningCache> caches = new LinkedHashMap<>();

	public synchronized ClassMetadataCache
			handle(ClassMetaRequest typedRequest) {
		// await update queue
		ClassMetadataCache result = new ClassMetadataCache();
		for (URL url : typedRequest.classPaths) {
			if (!caches.containsKey(url)) {
				ensureCache(url);
			}
			result.merge(caches.get(url).classes);
		}
		return result;
	}

	private void ensureCache(URL url) {
		try {
			ClasspathScanner scanner = new ClasspathScanner("*", true, true);
			scanner.invokeHandler(url);
			ClassMetadataCache classes = scanner.getClasses();
			ListeningCache cache = new ListeningCache(url, classes);
			caches.put(url, cache);
			cache.startListeners();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	class ListeningCache {
		private URL url;

		private ClassMetadataCache classes;

		public ListeningCache(URL url, ClassMetadataCache classes) {
			this.url = url;
			this.classes = classes;
		}

		public void startListeners() {
			if (url.getProtocol().equals("file")) {
				addPathListeners();
			}
		}

		private void addPathListeners() {
			String localPath = url.toString().replaceFirst("file:/+", "/");
			Path dir = Paths.get(localPath);
			new Thread() {
				public void run() {
					try {
						switch (SEUtilities.getOsType()) {
						case MacOS:
							new ClasspathWatchDirOsX(dir, true).processEvents();
							break;
						default:
							new ClasspathWatchDir(dir, true).processEvents();
							break;
						}
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				};
			}.start();
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
			ClasspathWatchDirOsX(Path dir, boolean recursive)
					throws IOException {
				super(dir, false);
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
				}
			}
		}

		public void refresh(String filePath, String relativeClassPath) {
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
}
