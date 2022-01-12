package cc.alcina.framework.common.client.collections;

import java.util.Collection;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.PropertyPath;

public class PropertyPathFilter<T> implements Predicate<T> {
	private PropertyPath accessor;

	private Object targetValue;

	private boolean targetIsCollection;

	private FilterOperator filterOperator;

	private PropertyFilter propertyFilter;

	public PropertyPathFilter() {
	}

	public PropertyPathFilter(String key, Object value) {
		this(key, value, FilterOperator.EQ);
	}

	public PropertyPathFilter(String propertyPath, Object propertyValue,
			FilterOperator filterOperator) {
		this.filterOperator = filterOperator;
		accessor = new PropertyPath(propertyPath);
		this.targetValue = propertyValue;
		targetIsCollection = targetValue instanceof Collection;
		this.propertyFilter = new PropertyFilter(propertyPath, propertyValue,
				filterOperator);
	}

	public PropertyPath getAccessor() {
		return this.accessor;
	}

	public FilterOperator getFilterOperator() {
		return this.filterOperator;
	}

	public PropertyFilter getPropertyFilter() {
		return this.propertyFilter;
	}

	public Object getTargetValue() {
		return this.targetValue;
	}

	@Override
	public boolean test(T o) {
		Object propertyValue = accessor.getChainedProperty(o);
		if (targetIsCollection && filterOperator == FilterOperator.EQ) {
			// FIXME - mvcc.5 - throw an exception (operator should be IN))
			return ((Collection) targetValue).contains(o);
		}
		if (propertyValue instanceof Collection) {
			for (Object item : (Collection) propertyValue) {
				if (propertyFilter.matchesValue(item)) {
					return true;
				}
			}
		}
		if (propertyFilter.matchesValue(propertyValue)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return Ax.format("Filter: %s%s%s", accessor.getPropertyPath(),
				filterOperator.operationText(), targetValue);
	}
}
