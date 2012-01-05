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
package cc.alcina.framework.common.client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.UnsafeNativeLong;

/**
 * 
 * @author Nick Reddel for legibility, and speed, provide both base64enc and
 *         plain versions
 */
public class SimpleStringParser {
	public static long longFromBase64(String value) {
		int pos = 0;
		long longVal = base64Value(value.charAt(pos++));
		int len = value.length();
		while (pos < len) {
			longVal <<= 6;
			longVal |= base64Value(value.charAt(pos++));
		}
		return longVal;
	}

	// Assume digit is one of [A-Za-z0-9$_]
	private static int base64Value(char digit) {
		if (digit >= 'A' && digit <= 'Z') {
			return digit - 'A';
		}
		// No need to check digit <= 'z'
		if (digit >= 'a') {
			return digit - 'a' + 26;
		}
		if (digit >= '0' && digit <= '9') {
			return digit - '0' + 52;
		}
		if (digit == '$') {
			return 62;
		}
		// digit == '_'
		return 63;
	}

	/**
	 * Return an optionally single-quoted string containing a base-64 encoded
	 * version of the given long value.
	 * 
	 * Keep this synchronized with the version in Base64Utils.
	 */
	private static String toBase64(long value) {
		// Convert to ints early to avoid need for long ops
		int low = (int) (value & 0xffffffff);
		int high = (int) (value >> 32);
		StringBuilder sb = new StringBuilder();
		boolean haveNonZero = base64Append(sb, (high >> 28) & 0xf, false);
		haveNonZero = base64Append(sb, (high >> 22) & 0x3f, haveNonZero);
		haveNonZero = base64Append(sb, (high >> 16) & 0x3f, haveNonZero);
		haveNonZero = base64Append(sb, (high >> 10) & 0x3f, haveNonZero);
		haveNonZero = base64Append(sb, (high >> 4) & 0x3f, haveNonZero);
		int v = ((high & 0xf) << 2) | ((low >> 30) & 0x3);
		haveNonZero = base64Append(sb, v, haveNonZero);
		haveNonZero = base64Append(sb, (low >> 24) & 0x3f, haveNonZero);
		haveNonZero = base64Append(sb, (low >> 18) & 0x3f, haveNonZero);
		haveNonZero = base64Append(sb, (low >> 12) & 0x3f, haveNonZero);
		base64Append(sb, (low >> 6) & 0x3f, haveNonZero);
		base64Append(sb, low & 0x3f, true);
		return sb.toString();
	}

	public static String toString(long value) {
		String serLong = GWT.isScript() ? fastMinSerialisedLong(value) : String
				.valueOf(value);
		return serLong + "/" + toBase64(value);
	}

	private static boolean base64Append(StringBuilder sb, int digit,
			boolean haveNonZero) {
		if (digit > 0) {
			haveNonZero = true;
		}
		if (haveNonZero) {
			int c;
			if (digit < 26) {
				c = 'A' + digit;
			} else if (digit < 52) {
				c = 'a' + digit - 26;
			} else if (digit < 62) {
				c = '0' + digit - 52;
			} else if (digit == 62) {
				c = '$';
			} else {
				c = '_';
			}
			sb.append((char) c);
		}
		return haveNonZero;
	}

	public int percentComplete() {
		return (offset * 100 + 1) / (s.length() + 1);
	}

	private final String s;

	@UnsafeNativeLong
	private static native String fastMinSerialisedLong(long value)/*-{
		if (!value.m&&!value.h){
			return value.l.toString();
		}else{
			return value.l.toString()+"(lsbits)";
		}
	}-*/;

	public static long toLong(String str) {
		int idx = str.indexOf("/");
		if (idx == -1) {
			return Long.parseLong(str);
		} else {
			if(str.substring(0,idx).contains(".")){
				return SimpleStringParser20.toLong(str);
			}
			return longFromBase64(str.substring(idx + 1));
		}
	}

	private int offset;

	public int getOffset() {
		return this.offset;
	}

	public SimpleStringParser(String s) {
		this.s = s;
		offset = 0;
	}

	public String read(String start, String end) {
		return read(start, end, false, true);
	}

	public String read(String start, String end,
			boolean includeStartTokenInText, boolean advanceEndToken) {
		if (offset >= s.length()) {
			return null;
		}
		int y1 = s.indexOf(start, offset);
		offset = end.length() == 0 ? s.length() : s.indexOf(end, y1
				+ start.length());
		String r = includeStartTokenInText ? s.substring(y1, offset == -1 ? s
				.length() : offset + end.length()) : s.substring(y1
				+ start.length(), offset == -1 ? s.length() : offset);
		if (advanceEndToken) {
			offset += end.length();
		}
		if (offset == -1) {
			offset = s.length();
		}
		return r;
	}

	public long readLong(String start, String end) {
		String str = read(start, end);
		return toLong(str);
	}
	public long readLongString(String start, String end) {
		String str = read(start, end);
		return Long.parseLong(str);
	}

	public int indexOf(String of) {
		return s.indexOf(of, offset);
	}
}
