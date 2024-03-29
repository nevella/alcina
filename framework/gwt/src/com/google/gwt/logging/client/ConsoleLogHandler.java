/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.logging.client;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A Handler that prints logs to the window.console.
 * <p>
 * Note we are consciously using 'window' rather than '$wnd' to avoid issues
 * similar to http://code.google.com/p/fbug/issues/detail?id=2914
 */
public class ConsoleLogHandler extends Handler {
	Boolean supported = null;

	public ConsoleLogHandler() {
		this(new TextLogFormatter(true), Level.ALL);
	}

	public ConsoleLogHandler(Formatter formatter, Level level) {
		setFormatter(formatter);
		setLevel(level);
	}

	@Override
	public void close() {
		// No action needed
	}

	private native void error(String message) /*-{
												window.console.error(message);
												}-*/;

	@Override
	public void flush() {
		// No action needed
	}

	private native void info(String message) /*-{
												window.console.info(message);
												}-*/;

	private native boolean isSupported() /*-{
											return !!window.console;
											}-*/;

	private native void log(String message) /*-{
											window.console.log(message);
											}-*/;

	@Override
	public void publish(LogRecord record) {
		if (supported == null) {
			try {
				supported = isSupported();
			} catch (Exception e) {
				supported = false;
			}
		}
		if (!supported || !isLoggable(record)) {
			return;
		}
		String msg = getFormatter().format(record);
		int val = record.getLevel().intValue();
		try {
			if (val >= Level.SEVERE.intValue()) {
				error(msg);
			} else if (val >= Level.WARNING.intValue()) {
				warn(msg);
			} else if (val >= Level.INFO.intValue()) {
				info(msg);
			} else {
				log(msg);
			}
		} catch (Exception e) {
			// hosted mode
			// e.printStackTrace();
		}
	}

	private native void warn(String message) /*-{
												window.console.warn(message);
												}-*/;
}
