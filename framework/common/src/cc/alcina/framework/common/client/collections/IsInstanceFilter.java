package cc.alcina.framework.common.client.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

public class IsInstanceFilter implements Predicate {
	private Collection<Class> classes;

	public IsInstanceFilter(Class... classes) {
		this.classes = Arrays.asList(classes);
	}

	public IsInstanceFilter(Collection<Class> classes) {
		this.classes = classes;
	}

	@Override
	public boolean test(Object o) {
		return o == null ? false : classes.contains(o.getClass());
	}
}