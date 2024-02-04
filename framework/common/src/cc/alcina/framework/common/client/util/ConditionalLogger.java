package cc.alcina.framework.common.client.util;

import java.util.function.Supplier;

import org.slf4j.Logger;

public class ConditionalLogger {
	private Logger logger;

	private Supplier<Boolean> test;

	public ConditionalLogger(Logger logger, Supplier<Boolean> test) {
		this.logger = logger;
		this.test = test;
	}

	public void debug(String format, Supplier<Object> supplier) {
		if (test.get()) {
			Object arg = supplier.get();
			if (arg != null && arg.getClass().isArray()) {
				logger.debug(format, (Object[]) arg);
			} else {
				logger.debug(format, arg);
			}
		}
	}
}
