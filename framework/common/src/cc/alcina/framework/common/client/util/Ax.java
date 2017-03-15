package cc.alcina.framework.common.client.util;

public class Ax {
	public static String format(String template, Object... args) {
		return CommonUtils.formatJ(template, args);
	}
}
