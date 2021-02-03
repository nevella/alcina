package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;

public class TextUtils {
	public static List<IntPair> findStringMatches(String text, String search) {
		int idx0 = 0;
		List<IntPair> result = new ArrayList<>();
		while (true) {
			int idx1 = text.indexOf(search, idx0);
			if (idx1 == -1 || search.length() == 0) {
				break;
			} else {
				result.add(new IntPair(idx1, idx1 + search.length()));
				idx0 = idx1 + search.length();
			}
		}
		return result;
	}

	public static int getWordCount(String data) {
		String normalised = normalizeWhitespaceAndTrim(data);
		return normalised.length() == 0 ? 0 : normalised.split(" ").length;
	}

	public static boolean isWhitespaceOrEmpty(String text) {
		return CommonUtils.isNullOrEmpty(text)
				|| normalizeWhitespaceAndTrim(text).length() == 0;
	}

	public static List<IntPair> match(String text, String regex) {
		if (text == null || regex == null) {
			return new ArrayList<IntPair>();
		}
		return TextUtilsImpl.match(text, regex);
	}

	public static String normalisedLcKey(String key) {
		return normalizeWhitespaceAndTrim(
				CommonUtils.nullToEmpty(key).toLowerCase());
	}

	public static String normalizeWhitespace(String text) {
		return TextUtilsImpl.normalizeWhitespace(text);
	}

	public static String normalizeWhitespaceAndTrim(String text) {
		return text == null ? null
				: TextUtilsImpl.normalizeWhitespace(text).trim();
	}

	public static String removeWhitespace(String text) {
		return normalizeWhitespace(text).replace(" ", "");
	}

	public static String trim(String key) {
		return CommonUtils.nullToEmpty(key).trim();
	}

	public static String trimOrNull(String key) {
		if (key == null) {
			return null;
		}
		return key.trim();
	}

	public static class Encoder {
		private static final String HEX = "0123456789ABCDEF";

		public static String decodeURIComponentEsque(String str) {
			if (str == null)
				return null;
			int length = str.length();
			byte[] bytes = new byte[length / 3];
			StringBuilder builder = new StringBuilder(length);
			try {
				for (int i = 0; i < length;) {
					char c = str.charAt(i);
					if (c != '%') {
						builder.append(c);
						i += 1;
					} else {
						int j = 0;
						do {
							char h = str.charAt(i + 1);
							char l = str.charAt(i + 2);
							i += 3;
							h -= '0';
							if (h >= 10) {
								h |= ' ';
								h -= 'a' - '0';
								if (h >= 6)
									throw new IllegalArgumentException();
								h += 10;
							}
							l -= '0';
							if (l >= 10) {
								l |= ' ';
								l -= 'a' - '0';
								if (l >= 6)
									throw new IllegalArgumentException();
								l += 10;
							}
							bytes[j++] = (byte) (h << 4 | l);
							if (i >= length)
								break;
							c = str.charAt(i);
						} while (c == '%');
						builder.append(new String(bytes, 0, j, "UTF-8"));
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			return builder.toString();
		}

		public static String encodeURIComponentEsque(String str) {
			if (str == null)
				return null;
			byte[] bytes = null;
			try {
				bytes = str.getBytes("UTF-8");
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			StringBuilder builder = new StringBuilder(bytes.length);
			for (byte c : bytes) {
				boolean escape = true;
				if (c >= 32 && c <= 126) {
					switch (c) {
					case '%':// escape
					case ';':// value separator
					case '+':// space
					case '&':// kv separator
						break;
					case ' ':
						escape = false;
						c = '+';
						break;
					default:
						escape = false;
						break;
					}
				}
				if (escape) {
					builder.append('%').append(HEX.charAt(c >> 4 & 0xf))
							.append(HEX.charAt(c & 0xf));
				} else {
					builder.append((char) c);
				}
			}
			return builder.toString();
		}
	}
}
