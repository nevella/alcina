package cc.alcina.framework.servlet.util.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Layout;
import org.apache.log4j.WriterAppender;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.servlet.logging.PerThreadLogging;
import cc.alcina.framework.servlet.logging.PerThreadLoggingHandler.PerThreadBuffer;

public class PerThreadAppender extends WriterAppender
		implements PerThreadLogging {
	private static final String CONTEXT_BUFFER = PerThreadAppender.class
			.getName() + ".CONTEXT_BUFFER";

	static OutputStreamWriter createWriter0(OutputStream os) {
		return new OutputStreamWriter(os, StandardCharsets.UTF_8) {
			@Override
			public void write(char[] cbuf, int off, int len)
					throws IOException {
				PerThreadBuffer buffer = LooseContext.get(CONTEXT_BUFFER);
				if (buffer != null) {
					String str = String.copyValueOf(cbuf, off, len);
					buffer.append(str);
				}
				// super.write(cbuf, off, len);
			}

			@Override
			public void write(String str) throws IOException {
				PerThreadBuffer buffer = LooseContext.get(CONTEXT_BUFFER);
				if (buffer != null) {
					buffer.append(str);
				}
				// super.write(str);
			}
		};
	}

	public PerThreadAppender(Layout layout) {
		super(layout, createWriter0(new ByteArrayOutputStream()));
	}

	@Override
	public void beginBuffer() {
		LooseContext.set(CONTEXT_BUFFER, new PerThreadBuffer());
	}

	@Override
	public String endBuffer() {
		PerThreadBuffer buffer = LooseContext.remove(CONTEXT_BUFFER);
		return buffer.toString();
	}
}
