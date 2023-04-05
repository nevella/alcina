package cc.alcina.framework.entity.gwt.reflection.impl;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.impl.ForName;

public class ForNameImpl implements ForName.Impl {
	private ClassLoader preferredClassloader;

	@Override
	public Class<?> forName(String fqn) {
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

	@Override
	public void init() {
		if (GWT.isClient()) {
			preferredClassloader = ForNameImpl.class.getClassLoader();
		} else {
			preferredClassloader = Thread.currentThread()
					.getContextClassLoader();
		}
	}
}
