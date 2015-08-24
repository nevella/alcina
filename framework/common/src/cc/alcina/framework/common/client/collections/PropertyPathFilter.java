package cc.alcina.framework.common.client.collections;

import java.util.Collection;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;

public class PropertyPathFilter<T> implements CollectionFilter<T> {
	private PropertyPathAccessor accessor;

	public PropertyPathAccessor getAccessor() {
		return this.accessor;
	}

	private Object targetValue;

	public Object getTargetValue() {
		return this.targetValue;
	}

	private boolean targetIsCollection;

	private FilterOperator filterOperator;

	public FilterOperator getFilterOperator() {
		return this.filterOperator;
	}

	private PropertyFilter propertyFilter;

	public PropertyFilter getPropertyFilter() {
		return this.propertyFilter;
	}

	private CollectionFilter contextFilter;

	public PropertyPathFilter() {
	}

	public PropertyPathFilter(String key, Object value) {
		this(key, value, FilterOperator.EQ);
	}

	public PropertyPathFilter(String propertyPath, Object propertyValue,
			FilterOperator filterOperator) {
		this.filterOperator = filterOperator;
		accessor = new PropertyPathAccessor(propertyPath);
		this.targetValue = propertyValue;
		targetIsCollection = targetValue instanceof Collection;
		this.propertyFilter = new PropertyFilter(propertyPath, propertyValue,
				filterOperator);
	}

	@Override
	public boolean allow(T o) {
		if (contextFilter != null) {
			return contextFilter.allow(o);
		}
		Object propertyValue = accessor.getChainedProperty(o);
		if (targetIsCollection && filterOperator == FilterOperator.EQ) {
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
	public void setContext(FilterContext context) {
		this.contextFilter = context.createContextFilter(this);
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Filter: %s%s%s",
				accessor.getPropertyPath(), filterOperator.operationText(),
				targetValue);
	}
}
