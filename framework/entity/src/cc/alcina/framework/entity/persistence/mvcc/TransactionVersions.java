package cc.alcina.framework.entity.persistence.mvcc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

class TransactionVersions {
	/*
	 * Only called by txmap old
	 */
	static List<Transaction> commonVisible(SortedSet<Transaction> set1,
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
	 * Avoids the use of size() on the (concurrent) sets, since that won't be a
	 * constant-time op.
	 *
	 * 'set2' will always only contain DOMAIN_COMMITTED transactions
	 * 
	 * TODO - document why these optimisation choices work (two cases - vacuum
	 * and 'resolve most recent')
	 */
	static Transaction mostRecentCommonVisible(NavigableSet<Transaction> set1,
			SortedSet<Transaction> set2) {
		Iterator<Transaction> itr2 = set2.iterator();
		Iterator<Transaction> itr1 = set1.iterator();
		if (itr1.hasNext() && itr2.hasNext()) {
			Transaction youngestSet2 = itr2.next();
			/*
			 * this (tailset) optimises largely set1-younger disjunct cases -
			 * such as vacuuming a few tx from a map txvalue with 100s of
			 * versions
			 */
			SortedSet<Transaction> tailSet = set1.tailSet(youngestSet2);
			itr1 = tailSet.iterator();
			itr2 = set2.iterator();
			if (itr1.hasNext()) {
				Transaction tx = null;
				while (itr1.hasNext() && itr2.hasNext()) {
					tx = itr1.next();
					if (set2.contains(tx)) {
						return tx;
					}
					tx = itr2.next();
					if (set1.contains(tx)) {
						return tx;
					}
				}
			}
		}
		return null;
	}
}
