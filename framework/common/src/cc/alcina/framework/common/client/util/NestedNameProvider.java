package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class NestedNameProvider {
	public static String get(Class clazz) {
		return get().getNestedSimpleName(clazz);
	}

	public static String get(Object object) {
		return object == null ? "null"
				: get().getNestedSimpleName(object.getClass());
	}

	private static NestedNameProvider get() {
		return Registry.impl(NestedNameProvider.class);
	}

	public String getNestedSimpleName(Class clazz) {
		String name = clazz.getName();
		int idx = name.lastIndexOf(".");
		if (idx == -1) {
			return name;
		} else {
			return name.substring(idx + 1).replace("$", ".");
		}
	}
}