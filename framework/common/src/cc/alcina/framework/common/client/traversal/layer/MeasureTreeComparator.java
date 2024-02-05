package cc.alcina.framework.common.client.traversal.layer;

import java.util.Comparator;

import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token.Order;
import cc.alcina.framework.common.client.util.Ax;

public class MeasureTreeComparator implements Comparator<MeasureSelection> {
	private Order order;

	public MeasureTreeComparator(Token.Order order) {
		this.order = order;
	}

	@Override
	public int compare(MeasureSelection o1, MeasureSelection o2) {
		{
			int cmp = o1.get().compareTo(o2.get(), false);
			if (cmp != 0) {
				return cmp;
			}
		}
		// order measures with range equality based on akbn
		// containment logic
		{
			int cmp = order.compare(o1.get().token, o2.get().token);
			if (cmp != 0) {
				return cmp;
			}
		}
		// try custom ordering on the type
		{
			int cmp = o1.equalRangeCompare(o2);
			if (cmp != 0) {
				return cmp;
			}
		}
		throw new IllegalStateException(Ax
				.format("Unable to order output measures: %s <=> %s", o1, o2));
	}
}