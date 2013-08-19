package cc.alcina.framework.common.client.collections;

import java.util.Collection;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.PropertyPathAccesor;

public class PropertyPathFilter<T> implements CollectionFilter<T> {
	public static final transient Object NOT_NULL = new Object();

	private PropertyPathAccesor accessor;

	private Object targetValue;

	private boolean targetIsCollection;

	public PropertyPathFilter() {
	}

	public PropertyPathFilter(String key, Object value) {
		accessor = new PropertyPathAccesor(key);
		this.targetValue = value;
		targetIsCollection = targetValue instanceof Collection;
	}

	@Override
	public boolean allow(T o) {
		Object propertyValue = accessor.getChainedProperty(o);
		if (targetIsCollection) {
			return ((Collection) targetValue).contains(o);
		}
		if (targetValue == NOT_NULL && propertyValue != null) {
			return true;
		}
		if (propertyValue instanceof Collection) {
			for (Object item : (Collection) propertyValue) {
				if (CommonUtils.equalsWithNullEquality(item, targetValue)) {
					return true;
				}
			}
		}
		if (CommonUtils.equalsWithNullEquality(propertyValue, targetValue)) {
			return true;
		}
		return false;
	}
}
