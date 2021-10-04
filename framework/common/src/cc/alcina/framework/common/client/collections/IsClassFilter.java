package cc.alcina.framework.common.client.collections;

import java.util.function.Predicate;

public class IsClassFilter implements Predicate {
	private Class clazz;

	public IsClassFilter(Class clazz) {
		this.clazz = clazz;
	}

	@Override
	public boolean test(Object o) {
		return o.getClass() == clazz;
	}
}