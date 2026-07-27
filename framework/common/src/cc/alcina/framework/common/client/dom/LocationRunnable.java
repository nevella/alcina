package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.dom.Location.IndexTuple;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Ax;

/**
 * TrackingLocationContext is nice - but works even better when mutations occur
 * in document order, after the mutation locations are computed
 */
@Bean(PropertySource.FIELDS)
public class LocationRunnable
		implements Comparable<LocationRunnable>, Runnable {
	public transient Location from;

	transient IndexTuple initialFrom;

	public transient Runnable runnable;

	public MutationEffect mutationEffect;

	public String description;

	public int index;

	public transient Location to;

	transient IndexTuple initialTo;

	_SerializableForm serializableForm;

	@Bean(PropertySource.FIELDS)
	static class _SerializableForm {
	}

	public void beforeSerialization() {
	}

	public void afterDeserialization() {
	}

	public LocationRunnable(int index, Location from, Location to,
			Runnable runnable, MutationEffect mutationEffect,
			String description) {
		this.index = index;
		this.from = from;
		this.to = to;
		this.runnable = runnable;
		this.mutationEffect = mutationEffect;
		this.description = description;
		this.initialFrom = from.asIndexTuple();
		this.initialTo = to == null ? null : to.asIndexTuple();
	}

	public enum MutationEffect {
		NEGATIVE_TREEINDEX_MUTATE_INDEX, NEGATIVE_TREE_INDEX_ZERO_INDEX,
		POSITIVE_TREE_INDEX_ZERO_INDEX, POSITIVE_TREE_INDEX_MUTATE_INDEX
	}

	@Override
	public String toString() {
		String initialToStr = initialTo == null ? ""
				: "-" + initialTo.toString();
		return Ax.format("%s - %s%s - %s", index, initialFrom, initialToStr,
				description);
	}

	@Override
	public int compareTo(LocationRunnable o) {
		{
			int cmp = from.compareTo(o.from);
			if (cmp != 0) {
				return cmp;
			}
		}
		{
			int cmp = mutationEffect.compareTo(o.mutationEffect);
			if (cmp != 0) {
				return cmp;
			}
		}
		return 0;
	}

	@Bean(PropertySource.FIELDS)
	public static class OrderedMutations {
		List<LocationRunnable> runnables = new ArrayList<>();

		public void run(boolean continueOnException) {
			runnables.sort(null);
			for (int idx = 0; idx < runnables.size(); idx++) {
				LocationRunnable locationRunnable = runnables.get(idx);
				if (continueOnException) {
					try {
						locationRunnable.run();
					} catch (Exception e) {
						Ax.out("DEVEX-1 - Location mutation exception");
						e.printStackTrace();
					}
				} else {
					locationRunnable.run();
				}
			}
		}

		public void add(Location from, Location to, Runnable runnable,
				MutationEffect mutationEffect, String description) {
			runnables.add(new LocationRunnable(runnables.size(), from, to,
					runnable, mutationEffect, description));
		}

		public void beforeSerialization() {
			runnables.forEach(LocationRunnable::beforeSerialization);
		}

		public void afterDeserialization() {
			runnables.forEach(LocationRunnable::afterDeserialization);
		}
	}

	@Override
	public void run() {
		runnable.run();
	}
}