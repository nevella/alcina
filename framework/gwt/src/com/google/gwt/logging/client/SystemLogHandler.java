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

import com.google.gwt.core.client.GWT;

/**
 * A Handler that prints logs to System.out or System.err.
 */
public class SystemLogHandler extends Handler {
	public SystemLogHandler() {
		this(new TextLogFormatter(true), Level.ALL);
	}

	public SystemLogHandler(Formatter formatter, Level level) {
		setFormatter(formatter);
		setLevel(level);
	}

	@Override
	public void close() {
		// No action needed
	}

	@Override
	public void flush() {
		// No action needed
	}

	private boolean isSupported() {
		return !GWT.isScript();
	}

	@Override
	public void publish(LogRecord record) {
		if (!isSupported() || !isLoggable(record)) {
			return;
		}
		String msg = getFormatter().format(record);
		int val = record.getLevel().intValue();
		if (val <= Level.WARNING.intValue()) {
			System.out.println(msg);
		} else {
			System.err.println(msg);
		}
	}
}
