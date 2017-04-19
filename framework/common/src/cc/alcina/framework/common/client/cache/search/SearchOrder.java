package cc.alcina.framework.common.client.cache.search;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId.HiliComparator;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;

@ClientInstantiable
public interface SearchOrder<T, V extends Comparable>
		extends Function<T, V>, Serializable, Comparator<T> {
	@Override
	default int compare(T o1, T o2) {
		int comparison = CommonUtils.compareWithNullMinusOne(apply(o1),
				apply(o2));
		if (comparison != 0) {
			return comparison;
		}
		return compare2(o1, o2);
	}

	default int compare2(T o1, T o2) {
		if (o1 instanceof HasIdAndLocalId) {
			return HiliComparator.INSTANCE.compare((HasIdAndLocalId) o1,
					(HasIdAndLocalId) o2);
		} else {
			return 0;
		}
	}
}
