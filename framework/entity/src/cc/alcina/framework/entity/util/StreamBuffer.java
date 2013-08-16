package cc.alcina.framework.entity.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamBuffer extends Thread {
	InputStream is;

	String type;

	StringBuilder buf = new StringBuilder();

	public StreamBuffer(InputStream is, String type) {
		this(is, type, true);
	}

	public StreamBuffer(InputStream is, String type, boolean sysout) {
		this.is = is;
		this.type = type;
	}

	boolean closed = false;

	public synchronized void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (buf.length() > 0) {
					buf.append("\n");
				}
				buf.append(line);
				System.out.println((type.isEmpty() ? "" : (type + ">")) + line);
			}
			closed = true;
			notifyAll();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public StringBuilder getBuf() {
		return this.buf;
	}

	public String getStreamResult() {
		return getBuf().toString();
	}

	private static final int TIMEOUT = 1 * 1000;

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
}