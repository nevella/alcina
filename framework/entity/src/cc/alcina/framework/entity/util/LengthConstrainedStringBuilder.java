package cc.alcina.framework.entity.util;

import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.util.LengthConstrainedStringWriter.OverflowException;

/**
 * StringBuilder is final (and in java.lang to boot), so no overriding there....
 *
 * @author nick@alcina.cc
 *
 */
public class LengthConstrainedStringBuilder {
	private StringBuilder builder = new StringBuilder();

	int maxLength = 10000000;

	public LengthConstrainedStringBuilder() {
	}

	public StringBuilder append(boolean b) {
		checkLength();
		return this.builder.append(b);
	}

	public StringBuilder append(char c) {
		checkLength();
		return this.builder.append(c);
	}

	public StringBuilder append(double d) {
		checkLength();
		return this.builder.append(d);
	}

	public StringBuilder append(float f) {
		checkLength();
		return this.builder.append(f);
	}

	public StringBuilder append(int i) {
		checkLength();
		return this.builder.append(i);
	}

	public StringBuilder append(long lng) {
		checkLength();
		return this.builder.append(lng);
	}

	public StringBuilder append(Object obj) {
		checkLength();
		return this.builder.append(obj);
	}

	public StringBuilder append(String str) {
		checkLength();
		return this.builder.append(str);
	}

	public boolean isEmpty() {
		return this.builder.length() == 0;
	}

	public int length() {
		return this.builder.length();
	}

	@Override
	public String toString() {
		checkLength();
		return this.builder.toString();
	}

	private void checkLength() {
		if (builder.length() > maxLength) {
			String first = "";
			String last = "";
			String buffer = builder.toString();
			if (buffer.length() <= 1000) {
				first = buffer.toString();
			} else {
				first = buffer.substring(0, 1000);
				last = buffer.substring(buffer.length() - 1000);
			}
			// stacktraces may be truncated - so print the top too
			List<StackTraceElement> frames = Arrays
					.asList(new Exception().getStackTrace());
			int fromIndex = Math.max(0, frames.size() - 200);
			List<StackTraceElement> topOfTrace = frames.subList(fromIndex,
					frames.size());
			throw new OverflowException(this.builder.toString(), Ax.format(
					"Limited-writer-overflow - %s bytes ::\n (0-1000): \n%s\n(last 1000)"
							+ ":\n%s\n\ntop of stack:\n%s",
					maxLength, first, last,
					CommonUtils.joinWithNewlines(topOfTrace)));
		}
	}
}
