package cc.alcina.framework.common.client.util;

public class Comparators {
	public static <C extends Comparable> C max(C c1, C c2) {
		return c1.compareTo(c2) > 0 ? c1 : c2;
	}

	public static <C extends Comparable> C min(C c1, C c2) {
		return c1.compareTo(c2) <= 0 ? c1 : c2;
	}
}
