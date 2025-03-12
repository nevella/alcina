package cc.alcina.framework.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
	public static Logger classLogger() {
		Class clazz = null;
		if (Configuration.useStackTraceCallingClass) {
			clazz = Configuration.getStacktraceCallingClass();
		} else {
			clazz = StackWalker
					.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
					.getCallerClass();
		}
		return getLogger(clazz);
	}

	static Map<Class, Logger> classLogger = new ConcurrentHashMap<>();

	/*
	 * I noticed huge numbers of loggers at times in Wildfly - so added this as
	 * a possible preventitive
	 */
	public static Logger getLogger(Class<?> clazz) {
		return classLogger.computeIfAbsent(clazz, LoggerFactory::getLogger);
	}
}
