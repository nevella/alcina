package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;

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
			return value.replaceFirst("^'(.+)'$", "$1");
		}
	}
}
