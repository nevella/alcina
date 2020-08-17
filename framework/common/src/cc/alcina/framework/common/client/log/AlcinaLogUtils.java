package cc.alcina.framework.common.client.log;

import java.util.Arrays;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.logging.client.SystemLogHandler;

import cc.alcina.framework.common.client.util.Ax;

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

	public static Logger getMetricLogger(Class clazz) {
		return getTaggedLogger(clazz, "metric");
	}

	public static Logger getTaggedLogger(Class clazz, String tag) {
		return LoggerFactory
				.getLogger(Ax.format("%s.__%s", clazz.getName(), tag));
	}

	public static void sysLogClient(Class clazz, Level level) {
		java.util.logging.Logger logger = java.util.logging.Logger
				.getLogger(clazz.getName());
		Arrays.stream(logger.getHandlers()).forEach(logger::removeHandler);
		logger.addHandler(
				new SystemLogHandler(new SimpleTextFormatter(false), level));
		logger.setUseParentHandlers(false);
	}
}
