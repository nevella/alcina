package cc.alcina.framework.entity.util;

import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.entity.SEUtilities;

public class Decl {
	public static Operation list(List list) {
		return new Operation().list(list);
	}

	public static class Operation {
		private List list;

		private String propertyName;

		public Operation filter(CollectionFilter filter) {
			list = CollectionFilters.filter(list, filter);
			return this;
		}

		public boolean firstGtSecond(double ratio) {
			return cmp(0, 1, ratio, true);
		}

		public boolean firstLtSecond(double ratio) {
			return cmp(0, 1, ratio, false);
		}

		public boolean firstSecondGtThird(double ratio) {
			return cmp(1, 2, 0, ratio, true);
		}

		public Operation list(List list) {
			this.list = list;
			return this;
		}

		public Operation property(String propertyName) {
			this.propertyName = propertyName;
			return this;
		}

		public boolean secondGtThird(double ratio) {
			return cmp(1, 2, ratio, true);
		}

		private boolean cmp(int i, int j, double ratio, boolean gt) {
			return cmp(i, j, -1, ratio, gt);
		}

		private boolean cmp(int i, int j, int i1, double ratio, boolean gt) {
			if (list.size() <= i || list.size() <= j || list.size() <= i1) {
				return false;
			}
			Number v1 = (Number) SEUtilities.getPropertyValue(list.get(i),
					propertyName);
			if (i1 >= 0) {
				v1 = v1.doubleValue()
						+ ((Number) SEUtilities.getPropertyValue(list.get(i1),
								propertyName)).doubleValue();
			}
			Number v2 = (Number) SEUtilities.getPropertyValue(list.get(j),
					propertyName);
			return gt ? v1.doubleValue() / v2.doubleValue() > ratio
					: v1.doubleValue() / v2.doubleValue() < ratio;
		}
	}
}
