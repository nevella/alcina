package cc.alcina.framework.common.client.collections;

import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.collections.CollectionFilters.ConverterFilter;

public class DistinctPropertyExtractor<T, V> implements ConverterFilter<T, V> {
	private String propertyName;

	private boolean allowNull = false;

	Set<V> returned = new LinkedHashSet<V>();

	public DistinctPropertyExtractor(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public boolean allowPostConvert(V c) {
		if (returned.contains(c) || (c == null && !allowNull)) {
			return false;
		}
		returned.add(c);
		return true;
	}

	@Override
	public boolean allowPreConvert(T t) {
		return true;
	}

	@Override
	public V convert(T original) {
		return (V) Reflections.propertyAccessor().getPropertyValue(original,
				propertyName);
	}

	public boolean isAllowNull() {
		return this.allowNull;
	}

	public void setAllowNull(boolean allowNull) {
		this.allowNull = allowNull;
	}
}