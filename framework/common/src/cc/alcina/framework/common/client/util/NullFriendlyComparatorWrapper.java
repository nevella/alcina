package cc.alcina.framework.common.client.util;

import java.util.Comparator;

/**
 * Null comes first...
 * 
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class NullFriendlyComparatorWrapper<T> implements Comparator<T> {
	private Comparator<T> wrappedComparator;

	public NullFriendlyComparatorWrapper() {
		this((Comparator<T>) Comparator.naturalOrder());
	}

	public NullFriendlyComparatorWrapper(Comparator<T> wrappedComparator) {
		this.wrappedComparator = wrappedComparator;
	}

	@Override
	public int compare(T o1, T o2) {
		if (o1 == null && o2 == null) {
			return 0;
		}
		if (o1 == null) {
			return -1;
		}
		if (o2 == null) {
			return 1;
		}
		return wrappedComparator.compare(o1, o2);
	}
}
