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

import cc.alcina.framework.common.client.service.CommonExtensions;

/**
 *
 * @author Nick Reddel
 */
public class SimpleStringParser20 {
	public static String longToGwtDoublesToString(long fieldValue) {
		return CommonExtensions.INSTANCE.longToGwtDoublesToString(fieldValue);
	}

	public static long toLong(String str) {
		return CommonExtensions.INSTANCE.gwtSerLongToString(str);
	}

	private final String s;

	private int offset;

	public SimpleStringParser20(String s) {
		this.s = s;
		offset = 0;
	}

	public int getOffset() {
		return this.offset;
	}

	public int indexOf(String of) {
		return s.indexOf(of, offset);
	}

	public int percentComplete() {
		return (offset * 100 + 1) / (s.length() + 1);
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
		offset = end.length() == 0 ? s.length()
				: s.indexOf(end, y1 + start.length());
		String r = includeStartTokenInText
				? s.substring(y1,
						offset == -1 ? s.length() : offset + end.length())
				: s.substring(y1 + start.length(),
						offset == -1 ? s.length() : offset);
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
}
