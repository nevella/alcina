package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface FileLogger {
	public static void log(String text) {
		Registry.optional(FileLogger.class).ifPresent(fl -> fl.logImpl(text));
	}

	void logImpl(String text);
}