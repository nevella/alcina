package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class NestedNameProvider {
	public static String get(Class clazz) {
		return get().getNestedSimpleName(clazz);
	}

	private static NestedNameProvider get() {
		return Registry.impl(NestedNameProvider.class);
	}

	public String getNestedSimpleName(Class clazz) {
		throw new UnsupportedOperationException();
	}
}