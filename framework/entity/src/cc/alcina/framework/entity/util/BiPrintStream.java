package cc.alcina.framework.entity.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.util.AlcinaConstants;

public class BiPrintStream extends PrintStream {
	public static String debugMarker = null;

	public PrintStream s1;

	public PrintStream s2;

	private PrintStream[] restoreStreams = null;

	public BiPrintStream() {
		super(new ByteArrayOutputStream());
	}

	@Override
	public PrintStream append(char c) {
		debugPrint();
		s2.append(c);
		return s1.append(c);
	}

	@Override
	public PrintStream append(CharSequence csq) {
		debugPrint(csq);
		s2.append(csq);
		return s1.append(csq);
	}

	@Override
	public PrintStream append(CharSequence csq, int start, int end) {
		debugPrint();
		s2.append(csq, start, end);
		return s1.append(csq, start, end);
	}

	@Override
	public void close() {
		s1.close();
		s2.close();
	}

	private void debugPrint() {
		int debug = 4;
	}

	private void debugPrint(Object obj) {
		if (debugMarker != null && obj != null) {
			String s = String.valueOf(obj);
			if (s != null) {
				boolean match = s.contains(debugMarker);
				try {
					match |= s.matches(debugMarker);
				} catch (Exception e) {
					throw WrappedRuntimeException.wrap(e);
				}
				if (match) {
					int debug = 4;
				}
			}
		}
	}

	@Override
	public void flush() {
		s1.flush();
		s2.flush();
	}

	@Override
	public PrintStream format(Locale l, String format, Object... args) {
		debugPrint(String.format(format, args));
		s2.format(l, format, args);
		return s1.format(l, format, args);
	}

	@Override
	public PrintStream format(String format, Object... args) {
		debugPrint(String.format(format, args));
		s2.format(format, args);
		return s1.format(format, args);
	}

	@Override
	public void print(boolean b) {
		debugPrint();
		s1.print(b);
		s2.print(b);
	}

	@Override
	public void print(char c) {
		debugPrint();
		s1.print(c);
		s2.print(c);
	}

	@Override
	public void print(char[] s) {
		debugPrint();
		s1.print(s);
		s2.print(s);
	}

	@Override
	public void print(double d) {
		debugPrint();
		s1.print(d);
		s2.print(d);
	}

	@Override
	public void print(float f) {
		debugPrint();
		s1.print(f);
		s2.print(f);
	}

	@Override
	public void print(int i) {
		debugPrint();
		s1.print(i);
		s2.print(i);
	}

	@Override
	public void print(long l) {
		debugPrint();
		s1.print(l);
		s2.print(l);
	}

	@Override
	public void print(Object obj) {
		debugPrint(obj);
		s1.print(obj);
		s2.print(obj);
	}

	@Override
	public void print(String s) {
		debugPrint(s);
		if (!LooseContext
				.is(AlcinaConstants.CONTEXT_ALCINA_DEBUG_DEV_LOGGING)) {
			s1.print(s);
			s2.print(s);
		}
	}

	@Override
	public PrintStream printf(Locale l, String format, Object... args) {
		debugPrint(String.format(format, args));
		s2.printf(l, format, args);
		return s1.printf(l, format, args);
	}

	@Override
	public PrintStream printf(String format, Object... args) {
		debugPrint(String.format(format, args));
		s2.printf(format, args);
		return s1.printf(format, args);
	}

	@Override
	public void println() {
		debugPrint();
		s1.println();
		s2.println();
	}

	@Override
	public void println(boolean x) {
		debugPrint();
		s1.println(x);
		s2.println(x);
	}

	@Override
	public void println(char x) {
		debugPrint();
		s1.println(x);
		s2.println(x);
	}

	@Override
	public void println(char[] x) {
		debugPrint();
		s1.println(x);
		s2.println(x);
	}

	@Override
	public void println(double x) {
		debugPrint();
		s1.println(x);
		s2.println(x);
	}

	@Override
	public void println(float x) {
		debugPrint();
		s1.println(x);
		s2.println(x);
	}

	@Override
	public void println(int x) {
		debugPrint();
		s1.println(x);
		s2.println(x);
	}

	@Override
	public void println(long x) {
		debugPrint();
		s1.println(x);
		s2.println(x);
	}

	@Override
	public void println(Object x) {
		debugPrint(x);
		if (!LooseContext
				.is(AlcinaConstants.CONTEXT_ALCINA_DEBUG_DEV_LOGGING)) {
			s1.println(x);
			s2.println(x);
		}
	}

	@Override
	public void println(String x) {
		debugPrint(x);
		s1.println(x);
		s2.println(x);
	}

	public void setMuted(boolean muted) {
		if (muted) {
			restoreStreams = new PrintStream[] { s1, s2 };
			s1 = new NullPrintStream();
			s2 = new NullPrintStream();
		} else {
			s1 = restoreStreams[0];
			s2 = restoreStreams[1];
			restoreStreams = null;
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		debugPrint();
		s1.write(b);
		s2.write(b);
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		String string = new String(Arrays.copyOfRange(buf, off, off + len));
		debugPrint(string);
		s1.write(buf, off, len);
		s2.write(buf, off, len);
	}

	@Override
	public void write(int b) {
		debugPrint();
		s1.write(b);
		s2.write(b);
	}

	public static class NullPrintStream extends PrintStream {
		public NullPrintStream() {
			super(new ByteArrayOutputStream());
		}
	}

	public static BiPrintStream swappablePrintStreams() {
		BiPrintStream result = new BiPrintStream();
		result.s1 = SwappableOutputStream.swappablePrintStream();
		result.s2 = SwappableOutputStream.swappablePrintStream();
		return result;
	}

	public enum StreamNumber {
		_1, _2
	}

	void redirect(StreamNumber streamNumber, PrintStream printStream) {
		switch (streamNumber) {
		case _1:
			s1 = SwappableOutputStream.swapPrintStream(s1, printStream);
			break;
		case _2:
			s2 = SwappableOutputStream.swapPrintStream(s2, printStream);
			break;
		}
	}
}