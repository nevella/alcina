package cc.alcina.framework.common.client.collections;

import java.util.Collection;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;

public class PropertyFilter<T> implements CollectionFilter<T> {
	public static final transient Object NOT_NULL = new Object();

	PropertyFilterTuple tuple;

	public PropertyFilter() {
	}

	public PropertyFilter(String key, Object value) {
		this(key, value, FilterOperator.EQ);
	}

	public PropertyFilter(String key, Object value, FilterOperator operator) {
		add(key, value, operator);
	}

	public PropertyFilter add(String key, Object value,
			FilterOperator operator) {
		tuple = new PropertyFilterTuple(key, value, operator);
		return this;
	}

	@Override
	public boolean allow(T o) {
		Object propertyValue = Reflections.propertyAccessor()
				.getPropertyValue(o, tuple.propertyName);
		boolean match = matchesValue(propertyValue);
		return match;
	}

	public boolean matchesValue(Object propertyValue) {
		boolean match = false;
		Object tupleValue = tuple.propertyValue;
		switch (tuple.operator) {
		case EQ:
			/*
			 * special-case querying with an int where the field is long
			 */
			if (propertyValue instanceof Long
					&& tupleValue instanceof Integer) {
				match = ((Long) propertyValue)
						.longValue() == ((Integer) tupleValue).longValue();
			} else {
				match = CommonUtils.equalsWithNullEquality(propertyValue,
						tupleValue);
			}
			break;
		case IS_NOT_NULL:
			match = propertyValue != null;
			break;
		case IS_NULL:
			match = propertyValue == null;
			break;
		case GT:
			match = propertyValue == null ? false
					: ((Comparable) propertyValue)
							.compareTo((Comparable) tupleValue) > 0;
			break;
		case LT:
			match = propertyValue == null ? false
					: ((Comparable) propertyValue)
							.compareTo((Comparable) tupleValue) < 0;
			break;
		case GT_EQ:
			match = propertyValue == null ? false
					: ((Comparable) propertyValue)
							.compareTo((Comparable) tupleValue) >= 0;
			break;
		case LT_EQ:
			match = propertyValue == null ? false
					: ((Comparable) propertyValue)
							.compareTo((Comparable) tupleValue) <= 0;
			break;
		case NE:
			match = !CommonUtils.equalsWithNullEquality(propertyValue,
					tupleValue);
			break;
		case MATCHES:
			if (propertyValue == null) {
				return false;
			}
			return propertyValue.toString().matches(tupleValue.toString());
		case IN: {
			if (propertyValue == null) {
				return false;
			}
			return ((Collection) tupleValue).contains(propertyValue);
		}
		}
		return match;
	}

	static class PropertyFilterTuple {
		String propertyName;

		Object propertyValue;

		FilterOperator operator;

		public PropertyFilterTuple(String propertyName, Object propertyValue,
				FilterOperator operator) {
			this.propertyName = propertyName;
			this.propertyValue = propertyValue;
			this.operator = operator;
		}
	}
}
