package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;

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

		@Override
		public String toString() {
			return key == null ? value : Ax.format("%s %s %s", key, op, value);
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
	}
}
