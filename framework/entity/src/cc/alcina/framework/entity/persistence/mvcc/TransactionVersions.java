package cc.alcina.framework.entity.persistence.mvcc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class TransactionVersions {
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
	 * Preferably have the smaller set in set1. We *really* don't want to have
	 * to get the size of a concurrent sortedsubset (since this involves
	 * traversal)
	 */
	public static Transaction mostRecentCommonVisible(
			SortedSet<Transaction> set1, SortedSet<Transaction> set2) {
		Transaction tx = null;
		Set<Transaction> larger = set2;
		Set<Transaction> smaller = set1;
		int set1size = set1.size();
		if (set1size > 20 && set1size > set2.size()) {
			larger = set1;
			smaller = set2;
		}
		Iterator<Transaction> itr = smaller.iterator();
		while (itr.hasNext()) {
			Transaction test = itr.next();
			if (larger.contains(test)) {
				tx = test;
				break;
			}
		}
		return tx;
	}
}
