package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class TextUtils {
	public static final String MONTHS = "January|February|March|April|May|June|"
			+ "July|August|September|October|November|December";

	public static final String DATE_REGEX_FULL = Ax
			.format("(\\d{1,2}) (%s) (\\d{4})\\s*", MONTHS);

	// keep in sync with normaliseWhitespaceOpt;
	public static final String WS_PATTERN_STR = "(?:[\\u0009\\u000A\\u000B\\u000C\\u000D\\u0020\\u00A0\\u0085\\u2000\\u2001\\u2002\\u2003])";

	public static final String NON_LINE_WS_PATTERN_STR = "(?:[\\u0009\\u000B\\u000C\\u0020\\u00A0\\u0085\\u2000\\u2001\\u2002\\u2003])";

	public static final RegExp WS_PATTERN = RegExp.compile(WS_PATTERN_STR + "+",
			"g");

	public static final RegExp WS_PATTERN_NON_MULTIPLE = RegExp
			.compile(WS_PATTERN_STR, "g");

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

	public static boolean isDateEn(String string) {
		return string != null && string.matches(DATE_REGEX_FULL);
	}

	public static boolean isWhitespaceOrEmpty(String text) {
		return CommonUtils.isNullOrEmpty(text)
				|| normalizeWhitespaceAndTrim(text).length() == 0;
	}

	public static List<IntPair> match(String text, String regex) {
		if (text == null || regex == null) {
			return new ArrayList<IntPair>();
		}
		RegExp regExp = RegExp.compile(regex, "ig");
		List<IntPair> result = new ArrayList<IntPair>();
		MatchResult matchResult;
		int idx = 0;
		while ((matchResult = regExp.exec(text)) != null) {
			result.add(new IntPair(matchResult.getIndex(),
					matchResult.getIndex() + matchResult.getGroup(0).length()));
		}
		return result;
	}

	public static String normalisedLcKey(String key) {
		return normalizeWhitespaceAndTrim(
				CommonUtils.nullToEmpty(key).toLowerCase());
	}

	private static String normaliseWhitespaceOpt(String input) {
		StringBuilder out = null;
		int len = input.length();
		boolean inWhitespace = false;
		boolean inWhitespacePrior = false;
		boolean ensureBuilder = false;
		for (int idx = 0; idx < len; idx++) {
			char c = input.charAt(idx);
			switch (c) {
			case '\u0009':
			case '\n':
			case '\u000B':
			case '\u000C':
			case '\r':
			case '\u00A0':
			case '\u0085':
			case '\u2000':
			case '\u2001':
			case '\u2002':
			case '\u2003':
				ensureBuilder = true;
				inWhitespace = true;
				break;
			case ' ':
				ensureBuilder |= inWhitespace;
				inWhitespace = true;
				break;
			default:
				inWhitespace = false;
				inWhitespacePrior = false;
				break;
			}
			if (inWhitespace) {
				if (ensureBuilder) {
					if (out == null) {
						out = new StringBuilder();
						out.append(input, 0, idx);
						// force append first time (may be 1 or 2 ws chars)
						out.append(' ');
						inWhitespacePrior = true;
					}
				}
				if (!inWhitespacePrior && out != null) {
					out.append(' ');
				}
				inWhitespacePrior = true;
			} else {
				if (out != null) {
					out.append(c);
				}
			}
		}
		return out != null ? out.toString() : input;
	}

	private static String normaliseWhitespaceOptNonMultiple(String input) {
		StringBuilder out = null;
		int len = input.length();
		boolean inWhitespace = false;
		boolean inWhitespacePrior = false;
		boolean ensureBuilder = false;
		for (int idx = 0; idx < len; idx++) {
			char c = input.charAt(idx);
			switch (c) {
			case '\u0009':
			case '\n':
			case '\u000B':
			case '\u000C':
			case '\r':
			case '\u00A0':
			case '\u0085':
			case '\u2000':
			case '\u2001':
			case '\u2002':
			case '\u2003':
				if (out == null) {
					out = new StringBuilder();
					out.append(input, 0, idx);
				}
				out.append(' ');
				break;
			default:
				if (out != null) {
					out.append(c);
				}
				break;
			}
		}
		return out == null ? input : out.toString();
	}

	public static String normalizeSpaces(String input) {
		return input == null ? null
				: GWT.isScript() ? WS_PATTERN_NON_MULTIPLE.replace(input, " ")
						: normaliseWhitespaceOptNonMultiple(input);
	}

	public static String normalizeWhitespace(String input) {
		return input == null ? null
				: GWT.isScript() ? WS_PATTERN.replace(input, " ")
						: normaliseWhitespaceOpt(input);
	}

	public static String normalizeWhitespaceAndTrim(String text) {
		return text == null ? null : normalizeWhitespace(text).trim();
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
		private static final char UNICODE_SMALL_AMPERSAND = '\uFE60';

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
					if (c == '+') {
						builder.append(' ');
						i += 1;
					} else if (c != '%') {
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
						String s = new String(bytes, 0, j, "UTF-8");
						builder.append(s);
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			return builder.toString().replace(UNICODE_SMALL_AMPERSAND, '&');
		}

		public static String encodeURIComponentEsque(String str) {
			if (str == null)
				return null;
			byte[] bytes = null;
			try {
				bytes = str.replace('&', UNICODE_SMALL_AMPERSAND)
						.getBytes("UTF-8");
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			StringBuilder builder = new StringBuilder(bytes.length);
			for (byte b : bytes) {
				char c = (char) b;
				boolean escape = true;
				if (c >= 32 && c <= 126) {
					switch (c) {
					case '%':// escape
					case ',':// value separator
					case '+':// space
					case ':':// k-v separator
					case '/':// possible path separator (url)
						break;
					// never reached
					// case '&':// query-string separator
					// // further escape because of issues with munging of '&'
					// // in hotmail etc
					// c = UNICODE_SMALL_AMPERSAND;
					// break;
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
