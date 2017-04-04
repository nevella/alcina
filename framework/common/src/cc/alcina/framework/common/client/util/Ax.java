package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public class Ax {
	public static String format(String template, Object... args) {
		return CommonUtils.formatJ(template, args);
	}

	public static String friendly(Object o) {
		return CommonUtils.friendlyConstant(o);
	}

	public static boolean isBlank(String string) {
		return CommonUtils.isNullOrEmpty(string);
	}

	public static String blankTo(String string, String defaultValue) {
		return isBlank(string) ? defaultValue : string;
	}

	public static String blankTo(String string,
			Supplier<String> defaultValueSupplier) {
		return isBlank(string) ? defaultValueSupplier.get() : string;
	}

	public static <T> Optional<T> first(Collection<T> collection) {
		return collection.size() == 0 ? Optional.empty()
				: Optional.of(collection.iterator().next());
	}

	public static boolean notBlank(String string) {
		return !isBlank(string);
	}
}
