package cc.alcina.framework.servlet.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class PerThreadLoggingHandler extends Handler {
	private static ThreadLocal<PerThreadBuffer> handlers = new ThreadLocal<>();

	public static void beginBuffer() {
		handlers.set(new PerThreadBuffer());
	}

	public static String endBuffer() {
		PerThreadBuffer buffer = handlers.get();
		if (buffer == null) {
			return "(Per-thread buffer already cleared)";
		} else {
			handlers.remove();
			return buffer.builder.toString();
		}
	}

	@Override
	public void close() throws SecurityException {
	}

	@Override
	public void flush() {
	}

	@Override
	public void publish(LogRecord record) {
		// publishes all events - doesn't filter by log level

		PerThreadBuffer buffer = handlers.get();
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