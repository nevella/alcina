package cc.alcina.framework.entity.util.fs;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;

import cc.alcina.framework.entity.SEUtilities;

public class FsUtils {
	public static WatchService newWatchService() throws IOException {
		switch (SEUtilities.getOsType()) {
		case MacOS:
			return new MacOsWatchService();
		default:
			return FileSystems.getDefault().newWatchService();
		}
	}

	public static boolean supportsTreeNotifications() {
		switch (SEUtilities.getOsType()) {
		case MacOS:
			return true;
		default:
			return false;
		}
	}

	public static Path toWatchablePath(Path path) {
		return new WatchablePath(path);
	}
}
