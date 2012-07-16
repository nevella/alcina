package cc.alcina.extras.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WeightAndDistribute<T> {
	public void run(Collection<T> items, WeightCallback<T> callback) {
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
				callback.weight(t, min
						+ (((double) ++i) / ((double) validCount))
						* (max - min));
			} else {
				callback.invalid(t);
			}
		}
	}

	public void distribute(Collection<T> items, CountingCallback<T> callback,
			int total) {
		int totalWeight = 0;
		int i = 0;
		for (T t : items) {
			if (callback.isValid(t)) {
				totalWeight += callback.itemWeight(t);
			}
		}
		if (total > items.size()) {
			for (T t : items) {
				callback.countingMap.add(t, 1);
				total--;
			}
		}
		for (T t : items) {
			if (callback.isValid(t)) {
				int itemWeight = callback.itemWeight(t) * total / totalWeight;
				callback.countingMap.add(t, itemWeight);
				total -= itemWeight;
			}
		}
		while (total > 0) {
			for (T t : items) {
				if (total-- > 0) {
					callback.countingMap.add(t, 1);
					
				}
			}
		}
	}

	public interface WeightCallback<T> extends Comparator<T> {
		double min();

		void weight(T t, double d);

		void invalid(T t);

		boolean isValid(T t);

		double max();

		int itemWeight(T t);
	}

	public abstract static class CountingCallback<T> implements
			WeightCallback<T> {
		CountingMap<T> countingMap = new CountingMap<T>();

		@Override
		public int compare(T o1, T o2) {
			// unused
			return 0;
		}

		@Override
		public double min() {
			// unused
			return 0;
		}

		@Override
		public void weight(T t, double d) {
			// unused
		}

		@Override
		public void invalid(T t) {
		}

		@Override
		public boolean isValid(T t) {
			return true;
		}

		@Override
		public double max() {
			return 0;
		}

		public CountingMap<T> getCountingMap() {
			return this.countingMap;
		}
	}
}
