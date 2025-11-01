package cc.alcina.framework.common.client.util;

import java.util.Comparator;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.reflection.Reflections;

/*
 * GWT-safe class utils
 */
public class ClassUtil {
	public static class SimpleNameComparator implements Comparator<Class> {
		@Override
		public int compare(Class o1, Class o2) {
			return o1.getSimpleName().compareTo(o2.getSimpleName());
		}
	}

	public static Class<?> resolveEnumSubclassAndSynthetic(Object obj) {
		if (obj == null) {
			return void.class;
		}
		return resolveEnumSubclassAndSynthetic(obj.getClass());
	}

	public static Class<?> resolveEnumSubclassAndSynthetic(Class clazz) {
		if (isEnumSubclass(clazz)) {
			return clazz.getSuperclass();
		}
		if (Al.isBrowser()) {
			return clazz;
		} else {
			// really, this should be injected by Domain (dependency)
			return Domain.resolveEntityClass(clazz);
		}
	}

	public static boolean isEnumish(Object test) {
		Class<? extends Object> clazz = test.getClass();
		return clazz.isEnum() || ClassUtil.isEnumSubclass(clazz);
	}

	public static boolean isEnumOrEnumSubclass(Class c) {
		return c.isEnum() || ClassUtil.isEnumSubclass(c);
	}

	public static boolean isEnumSubclass(Class c) {
		return c == null ? false
				: c.getSuperclass() != null && c.getSuperclass().isEnum();
	}

	public static boolean isDerivedFrom(Object o, Class c) {
		if (o == null) {
			return false;
		}
		Class c2 = o.getClass();
		while (c2 != Object.class) {
			if (c2 == c) {
				return true;
			}
			c2 = c2.getSuperclass();
		}
		return false;
	}

	// sync to
	// cc.alcina.framework.common.client.reflection.ClientReflections.getClassReflector(Class
	// clazz)
	public static boolean isImmutableJdkCollectionType(Class<?> clazz) {
		Class<?> cursor = clazz;
		while (cursor != null) {
			String className = cursor.getName();
			switch (cursor.getName()) {
			case "java.util.ImmutableCollections$AbstractImmutableList":
			case "java.util.Collections$UnmodifiableList":
			case "java.util.Collections$SingletonList":
			case "java.util.Collections$EmptyList":
			case "java.util.Collections$UnmodifiableRandomAccessList":
			case "java.util.Arrays$ArrayList":
				return true;
			}
			if (className.startsWith("java.util.ImmutableCollections$")) {
				return true;
			}
			cursor = cursor.getSuperclass();
		}
		return false;
	}

	public static Object fromStringValue(String stringValue, Class valueClass) {
		if (stringValue == null) {
			return stringValue;
		}
		if (valueClass == String.class) {
			return stringValue;
		}
		if (valueClass == Class.class) {
			return Reflections.forName(stringValue);
		}
		if (valueClass == Long.class || valueClass == long.class) {
			long id = Long.parseLong(stringValue);
			return id;
		}
		if (valueClass == Double.class || valueClass == double.class) {
			return Double.valueOf(stringValue);
		}
		if (valueClass == Integer.class || valueClass == int.class) {
			return Integer.valueOf(stringValue);
		}
		if (valueClass == Boolean.class || valueClass == boolean.class) {
			return Boolean.valueOf(stringValue);
		}
		if (Reflections.isAssignableFrom(Enum.class, valueClass)) {
			return CommonUtils.getEnumValueOrNull(valueClass, stringValue, true,
					null);
		}
		throw new UnsupportedOperationException();
	}

	public static boolean isSameClass(Object o1, Object o2) {
		Class clazz1 = o1 == null ? null : o1.getClass();
		Class clazz2 = o2 == null ? null : o2.getClass();
		return clazz1 == clazz2;
	}
}
