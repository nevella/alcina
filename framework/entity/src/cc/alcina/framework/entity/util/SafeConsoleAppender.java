package cc.alcina.framework.entity.util;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class SafeConsoleAppender extends ConsoleAppender {
	public SafeConsoleAppender() {
	}

	public SafeConsoleAppender(Layout layout) {
		super(layout);
	}

	@Override
	protected void subAppend(LoggingEvent event) {
		synchronized (SafeConsoleAppender.class) {
			this.qw.write(this.layout.format(event));
			if (event.getThrowableInformation() != null
					&& event.getThrowableInformation().getThrowable() != null) {
				if (layout.ignoresThrowable()) {
					String[] s = event.getThrowableStrRep();
					if (s != null) {
						int len = s.length;
						for (int i = 0; i < len; i++) {
							this.qw.write(s[i]);
							this.qw.write(Layout.LINE_SEP);
						}
					}
				}
			}
			if (shouldFlush(event)) {
				this.qw.flush();
			}
		}
	}
}