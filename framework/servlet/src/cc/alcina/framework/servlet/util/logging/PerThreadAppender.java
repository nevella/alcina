package cc.alcina.framework.servlet.util.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Layout;
import org.apache.log4j.WriterAppender;

import cc.alcina.framework.servlet.logging.PerThreadLogging;
import cc.alcina.framework.servlet.logging.PerThreadLoggingHandler.PerThreadBuffer;

public class PerThreadAppender extends WriterAppender
		implements PerThreadLogging {
	private static ThreadLocal<PerThreadBuffer> buffers = new ThreadLocal<>();

	static OutputStreamWriter createWriter0(OutputStream os) {
		return new OutputStreamWriter(os, StandardCharsets.UTF_8) {
			@Override
			public void write(char[] cbuf, int off, int len)
					throws IOException {
				PerThreadBuffer buffer = buffers.get();
				if (buffer != null) {
					String str = String.copyValueOf(cbuf, off, len);
					buffer.append(str);
				}
				// super.write(cbuf, off, len);
			}

			@Override
			public void write(String str) throws IOException {
				PerThreadBuffer buffer = buffers.get();
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
		if (buffers.get() != null) {
			throw new IllegalStateException("Per-thread buffer should be null");
		}
		buffers.set(new PerThreadBuffer());
	}

	@Override
	public String endBuffer() {
		PerThreadBuffer buffer = buffers.get();
		buffers.remove();
		return buffer.toString();
	}
}
