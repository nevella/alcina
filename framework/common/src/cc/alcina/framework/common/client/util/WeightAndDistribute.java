package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WeightAndDistribute<T> {
	public void run(Collection<T> items, WeightCallbacks<T> callback) {
		List<T> sorted = new ArrayList<T>(items);
		Collections.sort(sorted, callback);
		int validCount = 0;
		int i = 0;
		double min = callback.min();
		double max = callback.max();
		for (T t : sorted) {
			if (callback.isValid(t)) {
				validCount++;
			}
		}
		for (T t : sorted) {
			if (callback.isValid(t)) {
				callback.weight(t, min + (((double)++i) / ((double)validCount)) * (max - min));
			} else {
				callback.invalid(t);
			}
		}
	}

	public interface WeightCallbacks<T> extends Comparator<T> {
		double min();

		void weight(T t, double d);

		void invalid(T t);

		boolean isValid(T t);

		double max();
	}
}
