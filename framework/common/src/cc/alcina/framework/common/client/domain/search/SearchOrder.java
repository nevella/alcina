package cc.alcina.framework.common.client.domain.search;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;

@ClientInstantiable
public abstract class SearchOrder<T, V extends Comparable>
		implements Function<T, V>, Serializable, Comparator<T> {
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
