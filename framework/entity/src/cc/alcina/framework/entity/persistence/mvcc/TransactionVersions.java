package cc.alcina.framework.entity.persistence.mvcc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

class TransactionVersions {
	/*
	 * Only called by txmap old
	 */
	public static List<Transaction> commonVisible(SortedSet<Transaction> set1,
			SortedSet<Transaction> set2) {
		List<Transaction> result = new ArrayList<>();
		Set<Transaction> larger = null;
		Set<Transaction> smaller = null;
		if (set1.size() <= set2.size()) {
			larger = set2;
			smaller = set1;
		} else {
			larger = set1;
			smaller = set2;
		}
		Iterator<Transaction> itr = smaller.iterator();
		while (itr.hasNext()) {
			Transaction test = itr.next();
			if (larger.contains(test)) {
				result.add(test);
			}
		}
		return result;
	}

	/*
	 * Preferably have the smaller set in set1. Avoids the use of size() on the
	 * (concurrent) sets, since that won't be a constant-time op.
	 * 
	 * Strategy: try the first 10 of the (presumed smaller) set1, then switch to
	 * iterating over both sets
	 */
	public static Transaction mostRecentCommonVisible(
			SortedSet<Transaction> set1, SortedSet<Transaction> set2) {
		Transaction tx = null;
		Iterator<Transaction> itr1 = set1.iterator();
		Iterator<Transaction> itr2 = set2.iterator();
		int preferSet1 = 0;
		while (itr1.hasNext() && itr2.hasNext()) {
			tx = itr1.next();
			if (set2.contains(tx)) {
				return tx;
			}
			if (preferSet1++ > 10) {
				tx = itr2.next();
				if (set1.contains(tx)) {
					return tx;
				}
			}
		}
		return null;
	}
}
