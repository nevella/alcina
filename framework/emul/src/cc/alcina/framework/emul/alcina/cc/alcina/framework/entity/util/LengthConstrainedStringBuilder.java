package cc.alcina.framework.entity.util;

import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.util.LengthConstrainedStringWriter.OverflowException;

/**
 * StringBuilder is final (and in java.lang to boot), so no overriding there....
 * 
 * 
 *
 */
public class LengthConstrainedStringBuilder {
	private StringBuilder builder=new StringBuilder();

	int maxLength = 10000000;

	public LengthConstrainedStringBuilder() {
	}

	public boolean isEmpty() {
		return this.builder.length()==0;
	}

	public int length() {
		return this.builder.length();
	}

	public StringBuilder append(Object obj) {
		checkLength();
		return this.builder.append(obj);
	}

	public StringBuilder append(String str) {
		checkLength();
		return this.builder.append(str);
	}

	private void checkLength() {
		//noop
	}

	public StringBuilder append(boolean b) {
		checkLength();
		return this.builder.append(b);
	}

	public StringBuilder append(char c) {
		checkLength();
		return this.builder.append(c);
	}

	public StringBuilder append(int i) {
		checkLength();
		return this.builder.append(i);
	}

	public StringBuilder append(long lng) {
		checkLength();
		return this.builder.append(lng);
	}

	public StringBuilder append(float f) {
		checkLength();
		return this.builder.append(f);
	}

	public StringBuilder append(double d) {
		checkLength();
		return this.builder.append(d);
	}

	public String toString() {
		checkLength();
		return this.builder.toString();
	}
}
