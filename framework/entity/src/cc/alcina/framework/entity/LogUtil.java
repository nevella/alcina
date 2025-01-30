package cc.alcina.framework.entity;

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
		return LoggerFactory.getLogger(clazz);
	}
}
