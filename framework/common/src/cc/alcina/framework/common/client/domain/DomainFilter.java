package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.collections.PropertyPathFilter;
import cc.alcina.framework.common.client.util.Ax;

public class DomainFilter {
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

	public DomainFilter(String propertyPath, Object propertyValue) {
		this(propertyPath, propertyValue, FilterOperator.EQ);
	}

	public DomainFilter(String key, Object value, FilterOperator operator) {
		this.propertyPath = key;
		this.propertyValue = value;
		this.filterOperator = operator;
	}

	protected DomainFilter() {
	}

	public CollectionFilter asCollectionFilter() {
		return predicate != null ? new CollectionFilter() {
			@Override
			public boolean allow(Object o) {
				return predicate.test(o);
			}

			@Override
			public String toString() {
				return DomainFilter.this.toString();
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
}
