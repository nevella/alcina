package cc.alcina.framework.common.client.util;

import java.util.Comparator;

import cc.alcina.framework.common.client.domain.Domain;

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
}
