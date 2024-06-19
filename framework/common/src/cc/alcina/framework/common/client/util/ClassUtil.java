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

	public static Class resolveEnumSubclassAndSynthetic(Class clazz) {
		if (Al.isBrowser()) {
			return clazz;
		} else {
			// really, this should be injected by Domain (dependency)
			return Domain.resolveEntityClass(clazz);
		}
	}
}
