/* 
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
package cc.alcina.framework.entity.util;

import java.io.StringWriter;
import java.io.Writer;

import org.apache.log4j.Layout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

/**
 * 
 * @author Nick Reddel
 */
public class WriterAccessWriterAppender extends WriterAppender {
	private static final int MAX_LENGTH = 5000000;

	private StringWriter writerAccess;

	public static final String STRING_WRITER_APPENDER_KEY = "stringWriterAppender";

	public StringWriter getWriterAccess() {
		return this.writerAccess;
	}

	@Override
	protected void subAppend(LoggingEvent event) {
		subAppend0(event);
		if (writerAccess.getBuffer().length() > MAX_LENGTH) {
			// assume single-threaded
			writerAccess.getBuffer().setLength(0);
			super.subAppend(event);
			writerAccess.getBuffer().append("...truncated\n");
		}
	}

	protected void subAppend0(LoggingEvent event) {
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

	public void resetWriter() throws Exception {
		Writer newWriter = writerAccess.getClass().newInstance();
		setWriter(newWriter);
	}

	@Override
	public synchronized void setWriter(Writer writer) {
		if (!(writer instanceof StringWriter)) {
			throw new RuntimeException("writer must be a StringWriter");
		}
		this.writerAccess = (StringWriter) writer;
		super.setWriter(writer);
	}
}