package cc.alcina.framework.common.client.reflection;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class ForName {
	private static ClassLoader preferredClassloader;

	public static void
			setServletLayerClassloader(ClassLoader preferredClassloader) {
		ForName.preferredClassloader = preferredClassloader;
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
