package cc.alcina.framework.common.client.log;

import java.util.logging.LogRecord;

import com.google.gwt.logging.client.TextLogFormatter;

public class SimpleTextFormatter extends TextLogFormatter {
	public SimpleTextFormatter(boolean showStackTraces) {
		super(showStackTraces);
	}

	@Override
	public String format(LogRecord event) {
		StringBuilder message = new StringBuilder();
		message.append(
				"[" + event.getLoggerName().replaceFirst(".+\\.", "") + "] ");
		message.append(event.getMessage());
		return message.toString();
	}
}