package cc.alcina.framework.common.client.domain.search.criterion;

import java.util.Comparator;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

/*
 * Does not extend OrderCriterion because it has a more complex payload
 */
@TypeSerialization("propertyorder")
@Bean(PropertySource.FIELDS)
public class PropertyOrderCriterion extends SearchCriterion {
	public Order value = new Order();

	public static class PropertyOrderComparator implements Comparator {
		List<PropertyOrderCriterion> propertyOrders;

		public PropertyOrderComparator(
				List<PropertyOrderCriterion> propertyOrders) {
			this.propertyOrders = propertyOrders;
		}

		@Override
		public int compare(Object o1, Object o2) {
			for (PropertyOrderCriterion order : propertyOrders) {
				int cmp = order.value.compare(o1, o2);
				if (cmp != 0) {
					return cmp;
				}
			}
			return 0;
		}
	}

	@TypedProperties
	public static class Order extends Bindable.Fields
			implements TreeSerializable, Comparator {
		public Class<? extends Bindable> type;

		public String propertyName;

		public SearchCriterion.Direction direction;

		transient Property property;

		public boolean nullsFirst;

		@Override
		public int compare(Object o1, Object o2) {
			return direction.toComparatorMultiplier() * compare0(o1, o2);
		}

		int compare0(Object b1, Object b2) {
			if (property == null) {
				property = Reflections.at(type).property(propertyName);
			}
			Comparable o1 = (Comparable) property.get(b1);
			Comparable o2 = (Comparable) property.get(b2);
			if (o1 == null) {
				if (o2 == null) {
					return 0;
				}
				return nullsFirst ? -1 : 0;
			}
			if (o2 == null) {
				return nullsFirst ? 1 : 0;
			}
			Preconditions.checkState(o1.getClass() == o2.getClass());
			return o1.compareTo(o2);
		}
	}

	public boolean provideIsOrderingCriterion() {
		return true;
	}

	@Override
	public boolean provideIsFilteringCriterion() {
		return false;
	}

	public static PropertyOrderCriterion of(TypedProperty property,
			boolean ascending) {
		PropertyOrderCriterion result = new PropertyOrderCriterion();
		result.value.type = property.definingType;
		result.value.propertyName = property.name();
		result.value.direction = Direction.ofAscending(ascending);
		return result;
	}
}
