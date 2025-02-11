package cc.alcina.framework.servlet.component.traversal;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ClassUtil;

public class StandardLayerAttributes {
	@TypeSerialization("sort-selected-first")
	public static class SortSelectedFirst
			extends TraversalPlace.LayerAttributes.Attribute {
		public SortSelectedFirst() {
		}

		public SortSelectedFirst(boolean present) {
			this.present = present;
		}

		// a hack for flat-type-ser (which doesn't support plain existence -
		// yet)
		public boolean present;
	}

	@TypeSerialization("filter")
	public static class Filter
			extends TraversalPlace.LayerAttributes.Attribute {
		public String key;

		public FilterOperator op = FilterOperator.EQ;

		public String value;

		public String sortKey;

		public Direction sortDirection;

		@Override
		public String toString() {
			String rValue = value.contains(" ") ? Ax.format("'%s'", value)
					: value;
			String result = key == null ? rValue
					: Ax.format("%s %s %s", key, op, rValue);
			if (sortKey != null) {
				result = Ax.format("%s sort %s %s", result, sortKey,
						sortDirection.toAbbrevString().toLowerCase());
			}
			return result;
		}

		public Filter() {
		}

		public Filter(String key, FilterOperator op, String value) {
			this.key = key;
			this.op = op;
			this.value = value;
		}

		public static Filter of(String key, FilterOperator op, String value) {
			return new Filter(key, op, value);
		}

		public Filter withSort(String sortKey, Direction sortDirection) {
			Filter result = of(key, op, value);
			result.sortKey = sortKey;
			result.sortDirection = sortDirection;
			return result;
		}

		public String normalisedValue() {
			return value == null ? null
					: value.replaceFirst("^'(.+)'$", "public");
		}

		public DomainFilter toDomainFilter(Class<?> filteredClass) {
			String normalisedValue = normalisedValue();
			Property property = Reflections.at(filteredClass).property(key);
			switch (op) {
			case MATCHES:
				return new DomainFilter(
						new MatchesPredicate(property, normalisedValue));
			default:
				Object filterValue = ClassUtil.fromStringValue(normalisedValue,
						property.getType());
				return new DomainFilter(key, filterValue, op);
			}
		}

		class MatchesPredicate implements Predicate<Object> {
			Property property;

			String normalisedValue;

			Pattern pattern;

			MatchesPredicate(Property property, String normalisedValue) {
				this.property = property;
				pattern = Pattern.compile(normalisedValue,
						Pattern.CASE_INSENSITIVE);
				this.normalisedValue = normalisedValue.toLowerCase();
			}

			@Override
			public boolean test(Object t) {
				Object obj = property.get(t);
				if (obj == null) {
					return normalisedValue.equals("null");
				}
				return pattern.matcher(obj.toString()).find();
			}
		}
	}
}
