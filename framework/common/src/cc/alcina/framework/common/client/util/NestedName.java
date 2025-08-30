package cc.alcina.framework.common.client.util;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;

@Registration.Singleton
public class NestedName {
	public static NestedName instance;

	private static NestedName get() {
		if (instance == null) {
			instance = Registry.impl(NestedName.class);
		}
		return instance;
	}

	public static String get(Class clazz) {
		return get().getNestedSimpleName(clazz);
	}

	public static String get(Object object) {
		return object == null ? "null"
				: get().getNestedSimpleName(object.getClass());
	}

	public static String packageSegments(Object object, int segmentCount) {
		Class clazz = object instanceof Class ? (Class) object
				: object.getClass();
		List<String> segments = List
				.of(clazz.getPackage().getName().split("\\."));
		return segments.stream()
				.skip(Math.max(0, segments.size() - segmentCount))
				.limit(segmentCount).collect(Collectors.joining("."));
	}

	public String getNestedSimpleName(Class clazz) {
		String name = clazz.getName();
		return getNestedSimpleName(name);
	}

	String getNestedSimpleName(String name) {
		if (name == null) {
			return null;
		}
		int idx = name.lastIndexOf(".");
		if (idx == -1) {
			return name;
		} else {
			return name.substring(idx + 1).replace("$", ".");
		}
	}

	public static String getSimple(Object obj) {
		if (obj == null) {
			return "null";
		}
		if (obj instanceof Class) {
			return ((Class) obj).getSimpleName();
		}
		return obj.getClass().getSimpleName();
	}

	/**
	 * Transforms a full string classname to a nested name
	 */
	public static class StringTransformer
			implements ModelTransform<String, String> {
		@Override
		public String apply(String t) {
			return NestedName.get().getNestedSimpleName(t);
		}
	}
}