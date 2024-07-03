package cc.alcina.framework.entity.util.fs;

import static com.barbarysoftware.watchservice.StandardWatchEventKind.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.barbarysoftware.watchservice.ClosedWatchServiceException;
import com.barbarysoftware.watchservice.StandardWatchEventKind;
import com.barbarysoftware.watchservice.WatchEvent;
import com.barbarysoftware.watchservice.WatchService;
import com.barbarysoftware.watchservice.WatchableFile;
import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;

/*
 * Uses a single os thread, single jvm thread to monitor the resources (by
 * selecting the highest common path)
 */
class MacOsWatchService extends AbstractNonSunWatchService {
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	List<MacOsWatchKey> watchKeys = new ArrayList<>();

	File longestCommonFile;

	com.barbarysoftware.watchservice.WatchKey longestCommonKey;

	public MacOsWatchService() {
		watcherThread = new WatcherThread();
		watcherThread.start();
	}

	WatcherThread watcherThread;

	class WatcherThread extends Thread {
		com.barbarysoftware.watchservice.WatchService service;

		WatcherThread() {
			super("mac-os-fs-watchservice");
			setDaemon(true);
			newService();
		}

		@Override
		public void run() {
			while (true) {
				try {
					processEvents();
				} catch (Throwable e) {
					e.printStackTrace();
					throw new WrappedRuntimeException(e);
				}
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					throw WrappedRuntimeException.wrap(e);
				}
			}
		}

		public void close() throws IOException {
			service.close();
		}

		public com.barbarysoftware.watchservice.WatchKey take()
				throws InterruptedException {
			return service.take();
		}

		public void newService() {
			WatchService oldService = service;
			this.service = com.barbarysoftware.watchservice.WatchService
					.newWatchService();
			if (oldService != null) {
				try {
					oldService.close();
					oldService.watchedFiles.forEach(service::watch);
				} catch (Exception e) {
					throw WrappedRuntimeException.wrap(e);
				}
			}
		}
	}

	void handleEvent(com.barbarysoftware.watchservice.WatchKey key,
			WatchEvent<?> event, File file) {
		try {
			String canonicalPath = file.getCanonicalPath();
			Optional<MacOsWatchKey> nioKey = watchKeys.stream().filter(
					watchKey -> watchKey.matchesCanonicalPath(canonicalPath))
					.findFirst();
			// it may not be, since the commonpath is a super of the path
			// elements
			if (nioKey.isPresent()) {
				nioKey.get().signalEvent(translate(event.kind()),
						file.toPath());
			}
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	@Override
	void implClose() throws IOException {
		watcherThread.close();
	}

	void processEvents() {
		for (;;) {
			// wait for key to be signalled
			com.barbarysoftware.watchservice.WatchKey key;
			try {
				key = watcherThread.take();
			} catch (InterruptedException x) {
				return;
			} catch (ClosedWatchServiceException cwse) {
				return;
			}
			try {
				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind kind = event.kind();
					// TBD - provide example of how OVERFLOW event is handled
					if (kind == OVERFLOW) {
						continue;
					}
					// Context for directory entry event is the file name of
					// entry
					WatchEvent<File> ev = cast(event);
					File file = ev.context();
					handleEvent(key, event, file);
				}
			} finally {
				if (!isOpen()) {
					break;
				}
				// reset key and remove from set if directory no longer
				// accessible
				boolean valid = key.reset();
				if (!valid) {
					// all directories are inaccessible
					break;
				}
			}
		}
	}

	@Override
	WatchKey register(Path path, Kind<?>[] events, Modifier... modifers)
			throws IOException {
		File file = path.toFile().getCanonicalFile();
		if (file.isDirectory()) {
		} else {
			file = file.getParentFile();
		}
		boolean registerKey = true;
		if (longestCommonFile == null) {
			longestCommonFile = file;
		} else {
			int idx = 0;
			String p1 = file.getPath();
			String p2 = longestCommonFile.getPath();
			for (; idx < p1.length() && idx < p2.length()
					&& p1.charAt(idx) == p2.charAt(idx); idx++) {
			}
			File registerCommonFile = new File(p2.substring(0, idx));
			Preconditions.checkArgument(
					registerCommonFile.toString().length() > 1,
					"Common path must be non-root");
			if (registerCommonFile.equals(longestCommonFile)) {
				registerKey = false;
			} else {
				longestCommonFile = registerCommonFile;
			}
		}
		if (registerKey) {
			if (longestCommonKey != null) {
				longestCommonKey.cancel();
				watcherThread.newService();
			}
			WatchableFile key = new WatchableFile(longestCommonFile);
			longestCommonKey = key.register(watcherThread.service, ENTRY_CREATE,
					ENTRY_DELETE, ENTRY_MODIFY);
		}
		watcherThread.service.watch(file);
		MacOsWatchKey nioKey = new MacOsWatchKey(path, this);
		watchKeys.add(nioKey);
		return nioKey;
	}

	private Kind<?> translate(
			com.barbarysoftware.watchservice.WatchEvent.Kind<?> kind) {
		if (kind == StandardWatchEventKind.ENTRY_CREATE) {
			return StandardWatchEventKinds.ENTRY_CREATE;
		}
		if (kind == StandardWatchEventKind.ENTRY_DELETE) {
			return StandardWatchEventKinds.ENTRY_DELETE;
		}
		if (kind == StandardWatchEventKind.ENTRY_MODIFY) {
			return StandardWatchEventKinds.ENTRY_MODIFY;
		}
		if (kind == StandardWatchEventKind.OVERFLOW) {
			return StandardWatchEventKinds.OVERFLOW;
		}
		throw new UnsupportedOperationException();
	}

	static class MacOsWatchKey extends AbstractNonSunWatchKey {
		private String canonicalPath;

		protected MacOsWatchKey(Path dir, AbstractNonSunWatchService watcher) {
			super(dir, watcher);
			try {
				this.canonicalPath = dir.toFile().getCanonicalPath();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public boolean matchesCanonicalPath(String fileCanonicalPath) {
			return fileCanonicalPath.startsWith(canonicalPath);
		}

		@Override
		public void cancel() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isValid() {
			return true;
		}
	}
}