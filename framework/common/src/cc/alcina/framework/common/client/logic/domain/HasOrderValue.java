package cc.alcina.framework.common.client.logic.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;

public interface HasOrderValue {
	public static final Comparator<HasOrderValue> COMPARATOR = new Comparator<HasOrderValue>() {
		public int compare(HasOrderValue o1, HasOrderValue o2) {
			if (o1 == null) {
				return o2 == null ? 0 : -1;
			}
			if (o2 == null) {
				return 1;
			}
			int r = CommonUtils.compareWithNullMinusOne(o1.getOrderValue(),
					o2.getOrderValue());
			if (r != 0) {
				return r;
			}
			if (o1 instanceof HasId && o2 instanceof HasId) {
				return new Long(((HasId) o1).getId())
						.compareTo(((HasId) o2).getId());
			}
			return 0;
		}
	};

	public Integer getOrderValue();

	public void setOrderValue(Integer value);

	public static class HasOrderValueHelper {
		public static void insertAfter(Collection<? extends HasOrderValue> sibs,
				HasOrderValue current, HasOrderValue newHov, int delta) {
			List<HasOrderValue> tmp = new ArrayList(sibs);
			Collections.sort(tmp, COMPARATOR);
			int currentOv = CommonUtils.iv(current.getOrderValue());
			newHov.setOrderValue(currentOv + delta);
			int minDiff = 0;
			for (HasOrderValue hov : tmp) {
				int ov = CommonUtils.iv(hov.getOrderValue());
				if (ov > currentOv) {
					minDiff = Math.min(minDiff, currentOv);
				}
			}
			int add = minDiff != 0 && minDiff < 2 * delta
					? minDiff < delta ? 2 * delta : delta : 0;
			if (add != 0) {
				for (HasOrderValue hov : tmp) {
					int ov = CommonUtils.iv(hov.getOrderValue());
					if (ov > currentOv) {
						hov.setOrderValue(ov + add);
					}
				}
			}
		}

		public static int maxValue(Collection maybeHaveOrders, Object ignore) {
			int maxOrderValue = 0;
			for (Object o : maybeHaveOrders) {
				if (o == ignore || o == null) {
					continue;
				}
				if (o instanceof HasOrderValue) {
					HasOrderValue hov = (HasOrderValue) o;
					maxOrderValue = Math.max(maxOrderValue,
							CommonUtils.iv(hov.getOrderValue()));
				}
			}
			return maxOrderValue;
		}
	}
}
