package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.List;

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
		return TextUtilsImpl.normalizeWhitespace(text).trim();
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
}
