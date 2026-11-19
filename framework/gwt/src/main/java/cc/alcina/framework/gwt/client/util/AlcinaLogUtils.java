package cc.alcina.framework.gwt.client.util;

import java.util.Arrays;
import java.util.logging.Level;

import com.google.gwt.core.client.GWT;
import com.google.gwt.logging.client.SystemLogHandler;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class AlcinaLogUtils {
	public static void clearClientHandlers(Class clazz) {
		clearClientHandlers(clazz.getName());
	}

	public static void clearClientHandlers(String name) {
		java.util.logging.Logger logger = java.util.logging.Logger
				.getLogger(name);
		logger.setUseParentHandlers(false);
		Arrays.stream(logger.getHandlers()).forEach(logger::removeHandler);
	}

	public static void muteAllLogging(boolean muteAll) {
		Registry.impl(LogMuter.class).muteAllLogging(muteAll);
	}

	public static void sysLogClient(Class clazz, Level level) {
		if (GWT.isClient()) {
			java.util.logging.Logger logger = java.util.logging.Logger
					.getLogger(clazz.getName());
			Arrays.stream(logger.getHandlers()).forEach(logger::removeHandler);
			logger.addHandler(new SystemLogHandler(
					new SimpleTextFormatter(false), level));
			logger.setUseParentHandlers(false);
		}
	}

	public static void setLogLevelClient(String path, Level newLevel) {
		java.util.logging.Logger logger = java.util.logging.Logger
				.getLogger(path);
		logger.setLevel(newLevel);
	}

	@Registration.Singleton
	public static class LogMuter {
		public void muteAllLogging(boolean muteAll) {
			// noop
		}
	}
}
