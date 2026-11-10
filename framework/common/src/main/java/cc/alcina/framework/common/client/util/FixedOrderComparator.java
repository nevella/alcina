package cc.alcina.framework.common.client.util;

import java.util.Comparator;
import java.util.List;

public class FixedOrderComparator<E> implements Comparator<E> {
	List<?> objects;

	boolean classComparator;

	public FixedOrderComparator(List<?> objects, boolean classComparator) {
		this.objects = objects;
		this.classComparator = classComparator;
	}

	@Override
	public int compare(Object o1, Object o2) {
		Object n1 = normalise(o1);
		Object n2 = normalise(o2);
		int ord1 = ordinal(n1);
		int ord2 = ordinal(n2);
		return ord1 - ord2;
	}

	Object normalise(Object o1) {
		if (o1 == null || !classComparator) {
			return o1;
		}
		if (o1 instanceof Class) {
			return o1;
		} else {
			return o1.getClass();
		}
	}

	int ordinal(Object n1) {
		return objects.indexOf(n1);
	}
}
