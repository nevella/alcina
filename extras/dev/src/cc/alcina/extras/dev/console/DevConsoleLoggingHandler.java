package cc.alcina.extras.dev.console;

import java.io.OutputStream;
import java.util.logging.ConsoleHandler;

public class DevConsoleLoggingHandler extends ConsoleHandler {
	@Override
	protected synchronized void setOutputStream(OutputStream out)
			throws SecurityException {
		// hijack the superclass (which sets systems.err)
		super.setOutputStream(System.out);
		setFormatter(new DevConsoleLoggingFormatter());
	}
}
