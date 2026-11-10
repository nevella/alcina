package cc.alcina.framework.common.client.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

public class IsClassFilter<T> implements Predicate<Class<? extends T>> {
	private Collection<Class<? extends T>> classes;

	public IsClassFilter(Class<? extends T>... classes) {
		this.classes = Arrays.asList(classes);
	}

	public IsClassFilter(Collection<Class<? extends T>> classes) {
		this.classes = classes;
	}

	@Override
	public boolean test(Class<? extends T> t) {
		return classes.contains(t);
	}
}