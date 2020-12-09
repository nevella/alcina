package cc.alcina.framework.entity.persistence.mvcc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class TransactionVersions {
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

	public static Transaction mostRecentCommonVisible(
			SortedSet<Transaction> set1, SortedSet<Transaction> set2) {
		Transaction tx = null;
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
				tx = test;
				break;
			}
		}
		return tx;
	}
}
