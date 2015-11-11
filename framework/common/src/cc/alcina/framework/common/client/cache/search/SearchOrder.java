package cc.alcina.framework.common.client.cache.search;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;

import cc.alcina.framework.common.client.util.CommonUtils;

public interface SearchOrder<T> extends Function<T, Comparable>, Serializable,
		Comparator<T> {
	@Override
	default int compare(T o1, T o2) {
		return CommonUtils.compareWithNullMinusOne(apply(o1), apply(o2));
	}
}
