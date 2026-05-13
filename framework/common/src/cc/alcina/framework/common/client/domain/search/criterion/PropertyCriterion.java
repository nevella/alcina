package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.DomainCriterionFilter;
import cc.alcina.framework.common.client.domain.search.SearchContext;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/**
 * Does not generate eql, rather used in property queries. The
 * class/propertyname is verified before use (and this criterion will often be
 * internal-only)
 */
@TypeSerialization("propertyname")
@Bean(PropertySource.FIELDS)
public class PropertyCriterion extends SearchCriterion {
	public Filter value = new Filter();

	@TypedProperties
	public static class Filter extends Bindable.Fields
			implements TreeSerializable {
		public String propertyName;

		public String filterValue;
	}

	public PropertyCriterion() {
		setDisplayName("Property");
		setOperator(StandardSearchOperator.CONTAINS);
	}

	public interface Handler extends DomainCriterionFilter<PropertyCriterion> {
		@Override
		default DomainFilter getFilter(PropertyCriterion criterion) {
			Class<? extends Bindable> type = SearchContext.get().def
					.queriedBindableClass();
			Property property = Reflections.at(type)
					.property(criterion.value.propertyName);
			return DomainFilter.ofSearchProperty(property,
					criterion.getOperator().toFilterOperator(),
					criterion.value.filterValue);
		}
	}

	public static SearchCriterion of(PropertyEnum property,
			Object filterValue) {
		PropertyCriterion criterion = new PropertyCriterion();
		criterion.value.propertyName = property.name();
		criterion.value.filterValue = String.valueOf(filterValue);
		return criterion;
	}
}
