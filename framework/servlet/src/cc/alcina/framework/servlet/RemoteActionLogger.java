package cc.alcina.framework.servlet;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.util.SafeConsoleAppender;
import cc.alcina.framework.entity.util.WriterAccessWriterAppender;

public class RemoteActionLogger extends Logger {
	private WriterAccessWriterAppender writerAppender;

	protected RemoteActionLogger(String name) {
		super(name);
		try {
			injectBlankHierarchy();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		writerAppender = new WriterAccessWriterAppender();
		writerAppender.setWriter(new StringWriter());
		writerAppender.setLayout(RemoteActionLoggerProvider.layout);
		writerAppender
				.setName(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY);
		addAppender(writerAppender);
		setLevel(Level.DEBUG);
		if (Sx.isTest()) {
			SafeConsoleAppender consoleAppender = new SafeConsoleAppender(
					RemoteActionLoggerProvider.layout);
			addAppender(consoleAppender);
		}
	}

	public String flushLogger() {
		return resetLogBuffer();
	}

	public String getLoggerBufferContents() {
		return writerAppender.getWriterAccess().toString();
	}

	public int getLoggerBufferLength() {
		return writerAppender.getWriterAccess().getBuffer().length();
	}

	public String getLoggerBufferSubstring(int from, int to) {
		return writerAppender.getWriterAccess().getBuffer().substring(from, to);
	}

	public String resetLogBuffer() {
		String result = writerAppender.getWriterAccess().toString();
		try {
			writerAppender.resetWriter();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return result;
	}

	private void injectBlankHierarchy() throws Exception {
		Method method = Category.class.getDeclaredMethod("setHierarchy",
				new Class[] { LoggerRepository.class });
		method.setAccessible(true);
		method.invoke(this, new Object[] { new BlankHierarchy() });
	}

	static class BlankHierarchy implements LoggerRepository {
		@Override
		public void addHierarchyEventListener(HierarchyEventListener arg0) {
		}

		@Override
		public void emitNoAppenderWarning(Category arg0) {
		}

		@Override
		public Logger exists(String arg0) {
			return null;
		}

		@Override
		public void fireAddAppenderEvent(Category arg0, Appender arg1) {
		}

		@Override
		public Enumeration getCurrentCategories() {
			return null;
		}

		@Override
		public Enumeration getCurrentLoggers() {
			return null;
		}

		@Override
		public Logger getLogger(String arg0) {
			return null;
		}

		@Override
		public Logger getLogger(String arg0, LoggerFactory arg1) {
			return null;
		}

		@Override
		public Logger getRootLogger() {
			return null;
		}

		@Override
		public Level getThreshold() {
			return null;
		}

		@Override
		public boolean isDisabled(int arg0) {
			return false;
		}

		@Override
		public void resetConfiguration() {
		}

		@Override
		public void setThreshold(Level arg0) {
		}

		@Override
		public void setThreshold(String arg0) {
		}

		@Override
		public void shutdown() {
		}
	}
}
