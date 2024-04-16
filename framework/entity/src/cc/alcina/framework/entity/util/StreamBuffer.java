package cc.alcina.framework.entity.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Callback;

public class StreamBuffer implements Runnable {
	private static final int TIMEOUT = 1 * 1000;

	InputStream is;

	String type;

	StringBuilder buf = new StringBuilder();

	public interface LineCallback extends Callback<String> {
		public static class Noop implements LineCallback {
			@Override
			public void accept(String s) {
				//
			}
		}
	}

	LineCallback lineCallback;

	boolean closed = false;

	public StreamBuffer(InputStream is, LineCallback lineCallback) {
		this.is = is;
		this.lineCallback = lineCallback;
	}

	public StreamBuffer(InputStream is, String type) {
		this(is, new TabbedSysoutCallback(type));
	}

	public StringBuilder getBuf() {
		return this.buf;
	}

	public String getStreamResult() {
		return getBuf().toString();
	}

	@Override
	public synchronized void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			StringBuilder line = new StringBuilder();
			int in = -1;
			while ((in = isr.read()) != -1) {
				char c = (char) in;
				buf.append(c);
				line.append(c);
				if (c == '\n') {
					lineCallback.accept(line.toString());
					line = new StringBuilder();
				}
			}
			if (!line.isEmpty()) {
				lineCallback.accept(line.toString());
			}
			closed = true;
			notifyAll();
		} catch (IOException ioe) {
			if (Ax.blankToEmpty(ioe.getMessage())
					.matches("(Stream|Socket) closed")) {
				Ax.out("Stream closed");
			} else {
				ioe.printStackTrace();
			}
		}
	}

	public void waitFor() {
		while (!closed) {
			try {
				synchronized (this) {
					if (!closed) {
						wait(TIMEOUT);
					}
				}
			} catch (InterruptedException e) {
			}
		}
		return;
	}

	public void close() {
		if (!closed) {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}