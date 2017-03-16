package cc.alcina.framework.common.client.util;

public class Ax {
	public static String format(String template, Object... args) {
		return CommonUtils.formatJ(template, args);
	}

	public static String friendly(Object o) {
		return CommonUtils.friendlyConstant(o);
	}
}
