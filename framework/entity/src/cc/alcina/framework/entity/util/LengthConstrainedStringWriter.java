package cc.alcina.framework.entity.util;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class LengthConstrainedStringWriter extends StringWriter {
	private int maxLength;

	private boolean truncateAtMaxLength;

	public LengthConstrainedStringWriter() {
		this(10000000, false);
	}

	public LengthConstrainedStringWriter(int maxLength,
			boolean truncateAtMaxLength) {
		this.maxLength = maxLength;
		this.truncateAtMaxLength = truncateAtMaxLength;
	}

	@Override
	public StringWriter append(char c) {
		if (!checkLength(1)) {
			return this;
		}
		return super.append(c);
	}

	@Override
	public StringWriter append(CharSequence csq) {
		if (!checkLength(csq.length())) {
			return this;
		}
		return super.append(csq);
	}

	@Override
	public StringWriter append(CharSequence csq, int start, int end) {
		if (!checkLength(end - start)) {
			return this;
		}
		return super.append(csq, start, end);
	}

	@Override
	public void write(char[] cbuf, int off, int len) {
		if (!checkLength(len)) {
			return;
		}
		super.write(cbuf, off, len);
	}

	@Override
	public void write(int c) {
		if (!checkLength(1)) {
			return;
		}
		super.write(c);
	}

	@Override
	public void write(String str) {
		if (!checkLength(str.length())) {
			return;
		}
		super.write(str);
	}

	@Override
	public void write(String str, int off, int len) {
		if (!checkLength(len)) {
			return;
		}
		super.write(str, off, len);
	}

	private boolean checkLength(int len) {
		if (maxLength == 0) {
			return true;
		}
		if (getBuffer().length() + len > maxLength) {
			if (truncateAtMaxLength) {
				return false;
			}
			String first = "";
			String last = "";
			if (getBuffer().length() <= 1000) {
				first = getBuffer().toString();
			} else {
				first = getBuffer().substring(0, 1000);
				last = getBuffer().substring(getBuffer().length() - 1000);
			}
			// stacktraces may be truncated - so print the top too
			List<StackTraceElement> frames = Arrays
					.asList(new Exception().getStackTrace());
			int fromIndex = Math.max(0, frames.size() - 200);
			List<StackTraceElement> topOfTrace = frames.subList(fromIndex,
					frames.size());
			throw new OverflowException(Ax.format(
					"Limited-writer-overflow - %s bytes ::\n (0-1000): \n%s\n(last 1000)"
							+ ":\n%s\n\ntop of stack:\n%s",
					maxLength, first, last,
					CommonUtils.joinWithNewlines(topOfTrace)));
		} else {
			return true;
		}
	}

	public static class OverflowException extends RuntimeException {
		public OverflowException(String message) {
			super(message);
		}
	}
}