package cc.alcina.framework.common.client.reflection.impl;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class ForName {
	private static ClassLoader preferredClassloader;

	public static void init() {
		if (GWT.isClient()) {
			preferredClassloader = ForName.class.getClassLoader();
		} else {
			ForName.preferredClassloader = Thread.currentThread()
					.getContextClassLoader();
		}
	}

	public static Class<?> forName(String fqn) {
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
