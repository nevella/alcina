package cc.alcina.framework.entity.util.fs;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.SEUtilities;

public class FsUtils {
	public static WatchService newWatchService() {
		try {
			switch (SEUtilities.getOsType()) {
			case MacOS:
				return new MacOsWatchService();
			default:
				return FileSystems.getDefault().newWatchService();
			}
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
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

	private static Map<Class, WatchService> classService;

	public static synchronized WatchService watchServiceFor(Class<?> clazz) {
		switch (SEUtilities.getOsType()) {
		case MacOS:
			break;
		default:
			return newWatchService();
		}
		if (classService == null) {
			classService = new LinkedHashMap<>();
		}
		return classService.computeIfAbsent(clazz, c -> newWatchService());
	}
}
