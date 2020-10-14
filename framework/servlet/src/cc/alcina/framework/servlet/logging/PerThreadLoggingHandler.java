package cc.alcina.framework.servlet.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class PerThreadLoggingHandler extends Handler
		implements PerThreadLogging {
	private static ThreadLocal<PerThreadBuffer> buffers = new ThreadLocal<>();

	@Override
	public void beginBuffer() {
		if (buffers.get() != null) {
			throw new IllegalStateException("Per-thread buffer should be null");
		}
		buffers.set(new PerThreadBuffer());
	}

	@Override
	public void close() throws SecurityException {
	}

	@Override
	public String endBuffer() {
		PerThreadBuffer buffer = buffers.get();
		buffers.remove();
		return buffer.toString();
	}

	@Override
	public void flush() {
	}

	@Override
	public void publish(LogRecord record) {
		// publishes all events - doesn't filter by level
		PerThreadBuffer buffer = buffers.get();
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