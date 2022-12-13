package cc.alcina.framework.common.client.util;

import java.util.Comparator;

public class ComparableComparator implements Comparator {
	@Override
	public int compare(Object o1, Object o2) {
		if (o1 instanceof Comparable && o2 instanceof Comparable
				&& o1.getClass() == o2.getClass()) {
			return ((Comparable) o1).compareTo((Comparable) o2);
		} else {
			return 0;
		}
	}
}
