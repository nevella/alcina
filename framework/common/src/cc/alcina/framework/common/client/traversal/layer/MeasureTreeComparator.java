package cc.alcina.framework.common.client.traversal.layer;

import java.util.Comparator;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;

public class MeasureTreeComparator implements Comparator<MeasureSelection> {
	private Order order;

	public MeasureTreeComparator(Token.Order order) {
		this.order = order;
	}

	@Override
	public int compare(MeasureSelection o1, MeasureSelection o2) {
		if (o1 == o2) {
			return 0;
		}
		/*
		 * If one of the measures is empty and the index starts are equal, order
		 * by the token comparator
		 */
		IntPair pair1 = o1.get().toIntPair();
		IntPair pair2 = o2.get().toIntPair();
		if (IntPair.sameStartAndAtLeastOnePoint(pair1, pair2)) {
			// skip to order comparator
		} else {
			/*
			 * Compare only text index. The reason for this is that the
			 * Token.Order has a higher logical precedence than source tag order
			 * - particularly when truncation is afoot
			 */
			{
				int cmp = o1.get().compareTo(o2.get(), true);
				if (cmp != 0) {
					return cmp;
				}
			}
		}
		// order measures with index equality based on akbn
		// containment logic
		{
			int cmp = order.compare(o1.get().token, o2.get().token);
			if (cmp != 0) {
				return cmp;
			}
		}
		/*
		 * compare tree index
		 */
		{
			int cmp = o1.get().compareTo(o2.get(), false);
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