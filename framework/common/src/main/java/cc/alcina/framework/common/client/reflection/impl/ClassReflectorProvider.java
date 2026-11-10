package cc.alcina.framework.common.client.reflection.impl;

import cc.alcina.framework.common.client.reflection.ClassReflector;

public class ClassReflectorProvider {
	private static Impl impl;

	public static ClassReflector getClassReflector(Class clazz) {
		return impl.getClassReflector(clazz);
	}

	public static void setImpl(Impl impl) {
		ClassReflectorProvider.impl = impl;
	}

	public interface Impl {
		ClassReflector getClassReflector(Class clazz);
	}
}
