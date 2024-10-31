package com.google.gwt.dom.client;

import java.util.List;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.Al;

/**
 * This class bridges server + client-side JSNI invocation - it will invoke a
 * runnable browser-side, or reflectively
 */
public class JsInvoke {
	public static <T> T invoke(Supplier<T> supplier, Class clazz,
			String methodName) {
		return invoke(supplier, clazz, methodName, List.of(), List.of());
	}

	public static <T> T invoke(Supplier<T> supplier, Class clazz,
			String methodName, List<Class> parameterTypes,
			List<?> parameterValues) {
		if (Al.isBrowser()) {
			return supplier.get();
		} else {
			return null;
		}
	}
}
