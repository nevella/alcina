package cc.alcina.framework.common.client.collections;

import java.util.Collection;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class PropertyFilter<T> implements Predicate<T> {
	public static final transient Object NOT_NULL = new Object();

	PropertyFilterCondition condition;

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
		condition = new PropertyFilterCondition(key, value, operator);
		return this;
	}

	public boolean matchesValue(Object propertyValue) {
		boolean match = false;
		Object tupleValue = condition.propertyValue;
		switch (condition.operator) {
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

	@Override
	public boolean test(T o) {
		Object propertyValue = Reflections.at(o.getClass())
				.property(condition.propertyName).get(o);
		boolean match = matchesValue(propertyValue);
		return match;
	}

	@Override
	public String toString() {
		return condition.toString();
	}

	static class PropertyFilterCondition {
		String propertyName;

		Object propertyValue;

		FilterOperator operator;

		public PropertyFilterCondition(String propertyName,
				Object propertyValue, FilterOperator operator) {
			this.propertyName = propertyName;
			this.propertyValue = propertyValue;
			this.operator = operator;
		}

		@Override
		public String toString() {
			return Ax.format("%s %s %s", propertyName, operator, propertyValue);
		}
	}
}
