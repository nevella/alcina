package cc.alcina.framework.common.client.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * Verifies the total order of a comparator and a list of objects, by checking
 * that each combination of pairs is consistent
 * 
 * First, it orders say [A,B,C,D] - then checks that A lte C, B lte C, C lte D -
 * and that the comparisons are symmettrical
 */
public class VerifyTotalOrder {
	public <T> void verify(List<T> objects, Comparator<T> cmp) {
		Collections.sort(objects, cmp);
		// cmp.compare(objects.get(99), objects.get(100));
		for (int idx0 = 0; idx0 < objects.size() - 1; idx0++) {
			for (int idx1 = idx0 + 1; idx1 < objects.size(); idx1++) {
				T t0 = objects.get(idx0);
				T t1 = objects.get(idx1);
				int rFwd = cmp.compare(t0, t1);
				int rRev = cmp.compare(t1, t0);
				if (rFwd > 0 || rRev < 0) {
					cmp.compare(t0, t1);
					cmp.compare(t1, t0);
					throw new IllegalStateException(
							"Does not satisfy total order");
				}
			}
		}
	}
}
