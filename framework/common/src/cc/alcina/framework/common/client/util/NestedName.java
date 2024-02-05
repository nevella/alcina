package cc.alcina.framework.common.client.util;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class NestedName {
	public static String get(Class clazz) {
		return get().getNestedSimpleName(clazz);
	}

	public static String get(Object object) {
		return object == null ? "null"
				: get().getNestedSimpleName(object.getClass());
	}

	private static NestedName get() {
		return Registry.impl(NestedName.class);
	}

	public String getNestedSimpleName(Class clazz) {
		String name = clazz.getName();
		int idx = name.lastIndexOf(".");
		if (idx == -1) {
			return name;
		} else {
			return name.substring(idx + 1).replace("$", ".");
		}
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
}