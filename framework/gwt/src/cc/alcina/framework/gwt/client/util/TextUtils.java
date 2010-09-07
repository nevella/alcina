package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.util.CommonUtils;

public class TextUtils {
	public static String normaliseAndTrim(String text) {
		return TextUtilsImpl.normalise(text).trim();
	}

	public static boolean isWhitespaceOrEmpty(String text) {
		return CommonUtils.isNullOrEmpty(text)
				|| normaliseAndTrim(text).length() == 0;
	}
}
