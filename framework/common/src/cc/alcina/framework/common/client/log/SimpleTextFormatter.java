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
		// message.append(getRecordInfo(event, "\n"));
		message.append(event.getMessage());
		// if (showStackTraces && event.getThrown() != null) {
		// message.append("\n");
		// event.getThrown().printStackTrace(new
		// StackTracePrintStream(message));
		// }
		return message.toString();
	}
}