package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * TrackingLocationContext is nice - but works even better when mutations occur
 * in document order, after the mutation locations are computed
 */
public class LocationRunnable
		implements Comparable<LocationRunnable>, Runnable {
	public Location location;

	public Runnable runnable;

	public LocationRunnable(Location location, Runnable runnable) {
		this.location = location;
		this.runnable = runnable;
	}

	@Override
	public int compareTo(LocationRunnable o) {
		return location.compareTo(o.location);
	}

	public static class OrderedMutations {
		List<LocationRunnable> runnables = new ArrayList<>();

		public void run() {
			runnables.sort(null);
			for (int idx = 0; idx < runnables.size(); idx++) {
				runnables.get(idx).run();
			}
		}

		public void add(Location location, Runnable runnable) {
			runnables.add(new LocationRunnable(location, runnable));
		}
	}

	@Override
	public void run() {
		runnable.run();
	}
}