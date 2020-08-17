package cc.alcina.framework.common.client.log;

import java.util.Arrays;
import java.util.logging.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;

public class AlcinaLogUtils {
	public static void clearClientHandlers(Class clazz) {
		clearClientHandlers(clazz.getName());
	}

	public static void clearClientHandlers(String name) {
		java.util.logging.Logger logger = java.util.logging.Logger
				.getLogger(name);
		logger.setUseParentHandlers(false);
		Handler[] handlers = logger.getHandlers();
		Arrays.asList(handlers).stream()
				.forEach(handler -> logger.removeHandler(handler));
	}

	public static Logger getMetricLogger(Class clazz) {
		return getTaggedLogger(clazz, "metric");
	}

	public static Logger getTaggedLogger(Class clazz, String tag) {
		return LoggerFactory
				.getLogger(Ax.format("%s.__%s", clazz.getName(), tag));
	}
}
