package cc.alcina.framework.common.client.collections;

import java.util.Arrays;
import java.util.Collection;

public class IsClassesFilter implements CollectionFilter {
	private Collection<Class> classes;

	public IsClassesFilter(Collection<Class> classes) {
		this.classes = classes;
	}

	public IsClassesFilter(Class... classes) {
		this.classes = Arrays.asList(classes);
	}

	@Override
	public boolean allow(Object o) {
		return o != null && classes.contains(o.getClass());
	}
}