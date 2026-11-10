package cc.alcina.framework.common.client.reflection.impl;

public class ForName {
	private static Impl impl;

	public static Class<?> forName(String fqn) {
		return impl.forName(fqn);
	}

	public static void init() {
		impl.init();
	}

	public static void setImpl(Impl impl) {
		ForName.impl = impl;
	}

	public static interface Impl {
		Class<?> forName(String fqn);

		void init();
	}
}
