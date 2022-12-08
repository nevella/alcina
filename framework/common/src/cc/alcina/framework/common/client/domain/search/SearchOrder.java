package cc.alcina.framework.common.client.domain.search;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasEquivalence;

@Reflected
public abstract class SearchOrder<T, V extends Comparable>
		implements Function<T, V>, Serializable, Comparator<T>,
		HasEquivalence<SearchOrder> {
	public V comparable(T o) {
		return apply(o);
	}

	@Override
	public int compare(T o1, T o2) {
		int comparison = CommonUtils.compareWithNullMinusOne(comparable(o1),
				comparable(o2));
		if (comparison != 0) {
			return comparison;
		}
		return compare2(o1, o2);
	}

	@Override
	public int equivalenceHash() {
		return getClass().hashCode();
	}

	@Override
	public boolean equivalentTo(SearchOrder other) {
		return getClass() == other.getClass();
	}

	public String provideKey() {
		return getClass().getName();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	protected int compare2(T o1, T o2) {
		if (o1 instanceof Entity) {
			return Entity.EntityComparator.INSTANCE.compare((Entity) o1,
					(Entity) o2);
		} else {
			return 0;
		}
	}
}
