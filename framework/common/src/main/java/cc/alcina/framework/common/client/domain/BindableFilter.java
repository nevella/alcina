package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.regexp.shared.RegExp;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.collections.PropertyPathFilter;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ClassUtil;

/**
 * <p>
 * To debug domain search, use the testingPredicate as so
 * 
 * <pre>
 * <code>
 void debugSearch(BindableSearchDefinition def, Entity entity) {
		BindableFilter.testingPredicate = new TestingPredicate(e -> e == entity,
				null);
		BindableSearchHandler.get().searchModel(def).getQueriedResultObjects()
				.forEach(Ax::out);
	}				
 * 
 * </code>
 * </pre>
 */
public class BindableFilter {
	public static TestingPredicate testingPredicate;

	public static List<BindableFilter> fromKvs(Object... objects) {
		List<BindableFilter> result = new ArrayList<BindableFilter>();
		for (int i = 0; i < objects.length; i += 2) {
			result.add(new BindableFilter((String) objects[i], objects[i + 1]));
		}
		return result;
	}

	private String propertyPath;

	private Object propertyValue;

	private Predicate predicate;

	private FilterOperator filterOperator;

	protected BindableFilter() {
	}

	public BindableFilter(Predicate predicate) {
		this.predicate = predicate;
	}

	public BindableFilter(PropertyEnum propertyPath, Object value,
			FilterOperator operator) {
		this(propertyPath.name(), value, operator);
	}

	public BindableFilter(String propertyPath, Object propertyValue) {
		this(propertyPath, propertyValue, FilterOperator.EQ);
	}

	public BindableFilter(String propertyPath, Object value,
			FilterOperator operator) {
		this.propertyPath = propertyPath;
		this.propertyValue = value;
		this.filterOperator = operator;
	}

	public Predicate asPredicate() {
		Predicate predicate0 = asPredicate0();
		if (testingPredicate == null) {
			return predicate0;
		} else {
			return new TestingPredicate(testingPredicate.debugTest, predicate0);
		}
	}

	private Predicate asPredicate0() {
		return predicate != null ? new Predicate() {
			@Override
			public boolean test(Object o) {
				return predicate.test(o);
			}

			@Override
			public String toString() {
				return BindableFilter.this.toString();
			}
		} : new PropertyPathFilter(propertyPath, propertyValue, filterOperator);
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

	public BindableFilter invertIf(boolean invert) {
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
			return Ax.format("BindableFilter: %s - %s",
					predicate.getClass().getSimpleName(), predicate);
		}
		return Ax.format("BindableFilter: %s %s %s", propertyPath,
				filterOperator.operationText(), propertyValue);
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

	public static BindableFilter ofSearchProperty(Property property,
			FilterOperator operator, String filterString) {
		switch (operator) {
		case MATCHES:
			return new BindableFilter(
					new MatchesPredicate(property, filterString));
		case IN:
			if (property.getName().equals("id")) {
				if (property.getType() == long.class || Reflections
						.isAssignableFrom(Entity.class, property.getType())) {
					Object filterValue = TransformManager
							.idListToLongs(filterString);
					return new BindableFilter(property, filterValue, operator);
				} else {
					throw new UnsupportedOperationException();
				}
			} else {
				// force non-domain (xx.user for instance)
				return null;
			}
		default:
			Object filterValue = ClassUtil.fromStringValue(filterString,
					property.getType());
			return new BindableFilter(property, filterValue, operator);
		}
	}

	static class MatchesPredicate implements Predicate<Object> {
		Property property;

		String normalisedValue;

		RegExp regExp;

		MatchesPredicate(Property property, String normalisedValue) {
			this.property = property;
			regExp = RegExp.compile(normalisedValue, "i");
			this.normalisedValue = normalisedValue.toLowerCase();
		}

		@Override
		public boolean test(Object t) {
			Object obj = property.get(t);
			if (obj == null) {
				return normalisedValue.equals("null");
			}
			return regExp.exec(obj.toString()) != null;
		}
	}
}
