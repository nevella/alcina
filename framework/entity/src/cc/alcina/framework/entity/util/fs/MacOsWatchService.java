package cc.alcina.framework.entity.util.fs;

import static com.barbarysoftware.watchservice.StandardWatchEventKind.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.barbarysoftware.watchservice.ClosedWatchServiceException;
import com.barbarysoftware.watchservice.StandardWatchEventKind;
import com.barbarysoftware.watchservice.WatchEvent;
import com.barbarysoftware.watchservice.WatchableFile;

import cc.alcina.framework.common.client.WrappedRuntimeException;

/*
 * TODO - 2023 - could probably just have one thread for the whole app - how
 * nice would that be?
 */
class MacOsWatchService extends AbstractNonSunWatchService {
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	private com.barbarysoftware.watchservice.WatchService barbaryWatchService;

	private Map<com.barbarysoftware.watchservice.WatchKey, Path> keys = new ConcurrentHashMap<>();

	private Map<com.barbarysoftware.watchservice.WatchKey, MacOsWatchKey> nioKeys = new ConcurrentHashMap<>();

	public MacOsWatchService() {
		this.barbaryWatchService = com.barbarysoftware.watchservice.WatchService
				.newWatchService();
		new Thread() {
			@Override
			public void run() {
				setName("mac-os-fs-watchservice");
				try {
					processEvents();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			};
		}.start();
	}

	private void handleEvent(com.barbarysoftware.watchservice.WatchKey key,
			WatchEvent<?> event, File file) {
		MacOsWatchKey nioKey = nioKeys.get(key);
		nioKey.signalEvent(translate(event.kind()), file.toPath());
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

	@Override
	void implClose() throws IOException {
		barbaryWatchService.close();
	}

	void processEvents() {
		for (;;) {
			// wait for key to be signalled
			com.barbarysoftware.watchservice.WatchKey key;
			try {
				key = barbaryWatchService.take();
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
					keys.remove(key);
					// all directories are inaccessible
					if (keys.isEmpty()) {
						break;
					}
				}
			}
		}
	}

	@Override
	WatchKey register(Path path, Kind<?>[] events, Modifier... modifers)
			throws IOException {
		File file = path.toFile();
		if (file.isDirectory()) {
		} else {
			file = file.getParentFile();
		}
		WatchableFile key = new WatchableFile(file);
		com.barbarysoftware.watchservice.WatchKey barbaryKey = key.register(
				barbaryWatchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		keys.put(barbaryKey, path);
		MacOsWatchKey nioKey = new MacOsWatchKey(path, this, barbaryKey);
		nioKeys.put(barbaryKey, nioKey);
		return nioKey;
	}

	static class MacOsWatchKey extends AbstractNonSunWatchKey {
		private com.barbarysoftware.watchservice.WatchKey barbarykey;

		protected MacOsWatchKey(Path dir, AbstractNonSunWatchService watcher,
				com.barbarysoftware.watchservice.WatchKey barbaryKey) {
			super(dir, watcher);
			this.barbarykey = barbaryKey;
		}

		@Override
		public void cancel() {
			barbarykey.cancel();
		}

		@Override
		public boolean isValid() {
			return barbarykey.isValid();
		}
	}
}