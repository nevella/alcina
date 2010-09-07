package cc.alcina.framework.gwt.client.util;

import java.util.regex.Pattern;

public class TextUtilsImpl {
	public static final String WS_PATTERN_STR = "(?:[\\u0009\\u000A\\u000B\\u000C\\u000D\\u0020\\u00A0])+";

	public static final Pattern WS_PATTERN = Pattern.compile(WS_PATTERN_STR);

	public static String normalise(String input) {
		return WS_PATTERN.matcher(input).replaceAll(" ");
	}
}
