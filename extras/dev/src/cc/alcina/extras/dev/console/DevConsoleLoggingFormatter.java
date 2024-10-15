package cc.alcina.extras.dev.console;

import java.util.logging.LogRecord;

import cc.alcina.framework.common.client.util.FormatBuilder;

public class DevConsoleLoggingFormatter
		extends java.util.logging.SimpleFormatter {
	public DevConsoleLoggingFormatter() {
	}

	@Override
	public String format(LogRecord record) {
		FormatBuilder format = new FormatBuilder().withTrackNewlines(true);
		format.appendPadLeft(8, DevConsole.timeSinceStartup());
		format.format(" [%s] ", record.getLevel());
		format.padTo(20);
		format.line(record.getMessage());
		return format.toString();
	}
}
