package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.dom.Location.IndexTuple;
import cc.alcina.framework.common.client.util.Ax;

/**
 * TrackingLocationContext is nice - but works even better when mutations occur
 * in document order, after the mutation locations are computed
 */
public class LocationRunnable
		implements Comparable<LocationRunnable>, Runnable {
	public Location location;

	IndexTuple initialIndicies;

	public Runnable runnable;

	public MutationEffect mutationEffect;

	public String description;

	public int index;

	public LocationRunnable(int index, Location location, Runnable runnable,
			MutationEffect mutationEffect, String description) {
		this.index = index;
		this.location = location;
		this.runnable = runnable;
		this.mutationEffect = mutationEffect;
		this.description = description;
		this.initialIndicies = location.asIndexTuple();
	}

	public enum MutationEffect {
		NEGATIVE_TREEINDEX_MUTATE_INDEX, NEGATIVE_TREE_INDEX_ZERO_INDEX,
		POSITIVE_TREE_INDEX_ZERO_INDEX, POSITIVE_TREE_INDEX_MUTATE_INDEX
	}

	@Override
	public String toString() {
		return Ax.format("%s - %s - %s", index, initialIndicies, description);
	}

	@Override
	public int compareTo(LocationRunnable o) {
		{
			int cmp = location.compareTo(o.location);
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

	public static class OrderedMutations {
		List<LocationRunnable> runnables = new ArrayList<>();

		public void run() {
			runnables.sort(null);
			for (int idx = 0; idx < runnables.size(); idx++) {
				LocationRunnable locationRunnable = runnables.get(idx);
				if (idx == 8) {
					int debug = 3;
				}
				locationRunnable.run();
			}
		}

		public void add(Location location, Runnable runnable,
				MutationEffect mutationEffect, String description) {
			runnables.add(new LocationRunnable(runnables.size(), location,
					runnable, mutationEffect, description));
		}
	}

	@Override
	public void run() {
		runnable.run();
	}
}