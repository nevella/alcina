package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtilsImpl {
	public static final String WS_PATTERN_STR = "(?:[\\u0009\\u000A\\u000B\\u000C\\u000D\\u0020\\u00A0\\u0085])";

	public static final Pattern WS_PATTERN = Pattern
			.compile(WS_PATTERN_STR + "+");

	public static List<IntPair> match(String text, String regex) {
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(text);
		List<IntPair> result = new ArrayList<IntPair>();
		while (m.find()) {
			result.add(new IntPair(m.start(), m.end()));
		}
		return result;
	}

	public static String normalizeWhitespace(String input) {
		return input == null ? null : WS_PATTERN.matcher(input).replaceAll(" ");
	}
}
