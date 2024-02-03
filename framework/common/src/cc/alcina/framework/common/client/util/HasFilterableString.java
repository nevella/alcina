package cc.alcina.framework.common.client.util;

public interface HasFilterableString {
	public static String filterableString(Object o) {
		return filterableString(o, "(Undefined)");
	}

	public static String filterableString(Object o, String placeholderText) {
		if (o == null) {
			return placeholderText;
		}
		if (o instanceof HasFilterableString) {
			return ((HasFilterableString) o).toFilterableString();
		} else {
			return o.toString();
		}
	}

	public String toFilterableString();
}
