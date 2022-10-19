package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.collections.PropertyPathFilter;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.Ax;

public class DomainFilter {
	public static TestingPredicate testingPredicate;

	public static List<DomainFilter> fromKvs(Object... objects) {
		List<DomainFilter> result = new ArrayList<DomainFilter>();
		for (int i = 0; i < objects.length; i += 2) {
			result.add(new DomainFilter((String) objects[i], objects[i + 1]));
		}
		return result;
	}

	private String propertyPath;

	private Object propertyValue;

	private Predicate predicate;

	private FilterOperator filterOperator;

	public DomainFilter(Predicate predicate) {
		this.predicate = predicate;
	}

	public DomainFilter(PropertyEnum propertyPath, Object value,
			FilterOperator operator) {
		this(propertyPath.name(), value, operator);
	}

	public DomainFilter(String propertyPath, Object propertyValue) {
		this(propertyPath, propertyValue, FilterOperator.EQ);
	}

	public DomainFilter(String propertyPath, Object value,
			FilterOperator operator) {
		this.propertyPath = propertyPath;
		this.propertyValue = value;
		this.filterOperator = operator;
	}

	protected DomainFilter() {
	}

	public Predicate asPredicate() {
		Predicate predicate0 = asPredicate0();
		if (testingPredicate == null) {
			return predicate0;
		} else {
			return new TestingPredicate(testingPredicate.debugTest, predicate0);
		}
	}

	public boolean canFlatten() {
		return predicate == null;
	}

	public FilterOperator getFilterOperator() {
		return filterOperator;
	}

	public Predicate getPredicate() {
		return predicate;
	}

	public String getPropertyPath() {
		return propertyPath;
	}

	public Object getPropertyValue() {
		return propertyValue;
	}

	public DomainFilter invertIf(boolean invert) {
		if (invert) {
			predicate = predicate.negate();
		}
		return this;
	}

	public void setFilterOperator(FilterOperator filterOperator) {
		this.filterOperator = filterOperator;
	}

	public void setPredicate(Predicate predicate) {
		this.predicate = predicate;
	}

	public void setPropertyPath(String propertyPath) {
		this.propertyPath = propertyPath;
	}

	public void setPropertyValue(Object propertyValue) {
		this.propertyValue = propertyValue;
	}

	@Override
	public String toString() {
		if (predicate != null) {
			return Ax.format("DomainFilter: %s - %s",
					predicate.getClass().getSimpleName(), predicate);
		}
		return Ax.format("DomainFilter: %s %s %s", propertyPath,
				filterOperator.operationText(), propertyValue);
	}

	private Predicate asPredicate0() {
		return predicate != null ? new Predicate() {
			@Override
			public boolean test(Object o) {
				return predicate.test(o);
			}

			@Override
			public String toString() {
				return DomainFilter.this.toString();
			}
		} : new PropertyPathFilter(propertyPath, propertyValue, filterOperator);
	}

	public static class TestingPredicate implements Predicate {
		private Predicate debugTest;

		private Predicate wrappedPredicate;

		public TestingPredicate(Predicate debugTest, Predicate wrapped) {
			this.debugTest = debugTest;
			this.wrappedPredicate = wrapped;
		}

		@Override
		public boolean test(Object t) {
			boolean result = wrappedPredicate.test(t);
			if (!result && debugTest.test(t)) {
				String predicateName = wrappedPredicate.toString();
				boolean debug = true;
			}
			return result;
		}

		@Override
		public String toString() {
			return wrappedPredicate.toString();
		}
	}
}
