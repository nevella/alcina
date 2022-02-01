package cc.alcina.framework.common.client.reflection.impl;

import com.google.gwt.core.client.GwtScriptOnly;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.ClientReflections;

@GwtScriptOnly
public class ForName {
	public static void init() {
	}

	public static Class<?> forName(String fqn) {
		return ClientReflections.forName(fqn);
	}
}
