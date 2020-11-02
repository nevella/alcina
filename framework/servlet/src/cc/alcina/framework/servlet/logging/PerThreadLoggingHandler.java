package cc.alcina.framework.servlet.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import cc.alcina.framework.common.client.util.LooseContext;

public class PerThreadLoggingHandler extends Handler
		implements PerThreadLogging {
	private static final String CONTEXT_BUFFER = PerThreadLoggingHandler.class
			.getName() + ".CONTEXT_BUFFER";

	@Override
	public void beginBuffer() {
		LooseContext.set(CONTEXT_BUFFER, new PerThreadBuffer());
	}

	@Override
	public void close() throws SecurityException {
	}

	@Override
	public String endBuffer() {
		PerThreadBuffer buffer = LooseContext.remove(CONTEXT_BUFFER);
		return buffer.toString();
	}

	@Override
	public void flush() {
	}

	@Override
	public void publish(LogRecord record) {
		// publishes all events - doesn't filter by log level
		PerThreadBuffer buffer = LooseContext.get(CONTEXT_BUFFER);
		if (buffer != null) {
			buffer.publish(record);
		}
	}

	public static class PerThreadBuffer {
		private StringBuilder builder = new StringBuilder();

		public void append(String string) {
			builder.append(string);
		}

		public void publish(LogRecord record) {
			builder.append(record.getMessage());
			builder.append("\n");
		}

		@Override
		public String toString() {
			return builder.toString();
		}
	}
}