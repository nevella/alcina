package cc.alcina.framework.entity.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;

/*
 * All methods are synchronized (to ensure swap is safe)
 */
class SwappableOutputStream extends OutputStream {
	OutputStream delegate;

	SwappableOutputStream() {
		delegate = new ByteArrayOutputStream();
	}

	@Override
	public synchronized void write(byte[] b) throws IOException {
		this.delegate.write(b);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len)
			throws IOException {
		this.delegate.write(b, off, len);
	}

	@Override
	public synchronized void flush() throws IOException {
		this.delegate.flush();
	}

	@Override
	public synchronized void close() throws IOException {
		this.delegate.close();
	}

	public synchronized void swap(OutputStream newOutputStream)
			throws IOException {
		Preconditions.checkState(delegate instanceof ByteArrayOutputStream,
				"Swap to non-ByteArrayOutputStream can only be called once");
		ByteArrayOutputStream baos = (ByteArrayOutputStream) delegate;
		baos.writeTo(newOutputStream);
	}

	@Override
	public synchronized void write(int b) throws IOException {
		this.delegate.write(b);
	}

	static SwappablePrintStream swappablePrintStream() {
		SwappableOutputStream outputStream = new SwappableOutputStream();
		return new SwappablePrintStream(outputStream);
	}

	static class SwappablePrintStream extends PrintStream {
		SwappableOutputStream swappableOutputStream;

		SwappablePrintStream(SwappableOutputStream swappableOutputStream) {
			super(swappableOutputStream);
			this.swappableOutputStream = swappableOutputStream;
		}
	}

	public static PrintStream swapPrintStream(PrintStream from,
			PrintStream to) {
		if (from instanceof SwappablePrintStream) {
			SwappablePrintStream sps = (SwappablePrintStream) from;
			try {
				sps.swappableOutputStream.swap(to);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return to;
	}
}