package cc.alcina.framework.common.client.reflection;

import cc.alcina.framework.common.client.WrappedRuntimeException;

class ForName {
	private static ClassLoader preferredClassloader;

	static void init() {
		ForName.preferredClassloader = Thread.currentThread()
				.getContextClassLoader();
	}

	static Class<?> forName(String fqn) {
		try {
			if (preferredClassloader != null) {
				try {
					return preferredClassloader.loadClass(fqn);
				} catch (Exception e) {
					return Class.forName(fqn);
				}
			} else {
				return Class.forName(fqn);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
