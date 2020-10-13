package cc.alcina.framework.servlet.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class PerThreadLoggingHandler extends Handler {
	private static ThreadLocal<PerThreadBuffer> handlers = new ThreadLocal<>();

	public static void beginBuffer() {
		if (handlers.get() != null) {
			throw new IllegalStateException("Per-thread buffer should be null");
		}
		handlers.set(new PerThreadBuffer());
	}

	public static String endBuffer() {
		PerThreadBuffer buffer = handlers.get();
		handlers.remove();
		return buffer.builder.toString();
	}

	@Override
	public void close() throws SecurityException {
	}

	@Override
	public void flush() {
	}

	@Override
	public void publish(LogRecord record) {
		PerThreadBuffer buffer = handlers.get();
		if (buffer != null) {
			buffer.publish(record);
		}
	}

	public static class PerThreadBuffer {
		private StringBuilder builder = new StringBuilder();

		public void publish(LogRecord record) {
			builder.append(record.getMessage());
			builder.append("\n");
		}
	}
}