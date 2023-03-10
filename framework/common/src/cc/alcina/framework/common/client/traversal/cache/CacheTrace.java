package cc.alcina.framework.common.client.traversal.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;

/**
 * <p>
 * Lightweight cache debugging. Since code affecting the cache operation can
 * occur on sibling branches in the stack, observe in a parent context frame via
 * something like the following:
 *
 * <pre>
 * <code>
 * CacheTrace trace = new CacheTrace("my-event-name",true);
 * ProcessObservers.context()
  .observe(new CacheTrace.CacheEventObserver(trace));</code>
 * </pre>
 *
 * which will hook the {@code CacheTrace} up to any events published via
 * {@code CacheEvent.publish()}
 */
public class CacheTrace {
	private final boolean enabled;

	List<String> traces;

	private final String name;

	public boolean hit;

	public CacheTrace(String name, boolean enabled) {
		this.name = name;
		this.enabled = enabled;
		if (enabled) {
			traces = new ArrayList<>();
		}
	}

	public void info(String template, Supplier<?>... argSuppliers) {
		if (!enabled) {
			return;
		}
		Object[] args = new Object[argSuppliers.length];
		for (int idx = 0; idx < argSuppliers.length; idx++) {
			Supplier<?> supplier = argSuppliers[idx];
			args[idx] = supplier.get();
		}
		traces.add(Ax.format(template, args));
	}

	@Override
	public String toString() {
		return enabled
				? Ax.format("Name: %s\nTraces:\n  %s\n", name,
						traces.stream().collect(Collectors.joining("\n  ")))
				: "<disabled>";
	}

	public static class CacheEvent implements ProcessObservable {
		public static void publish(String template,
				Supplier<?>... argSuppliers) {
			ProcessObservers.context()
					.publish(new CacheEvent(template, argSuppliers));
		}

		public static void publishHit() {
			ProcessObservers.context().publish(new CacheEvent(true));
		}

		private String template;

		private Supplier<?>[] argSuppliers;

		boolean hit;

		public CacheEvent(boolean hit) {
			this.hit = hit;
		}

		public CacheEvent(String template, Supplier<?>... argSuppliers) {
			this.template = template;
			this.argSuppliers = argSuppliers;
		}
	}

	public static class CacheEventObserver implements
			ProcessObserver<CacheEvent>, TopicListener.HandlesSubscription {
		private CacheTrace trace;

		// for observer registries, it's simplest to watch for unsubscription,
		// since that's context-driven
		public Topic<Void> topicUnsubscribed = Topic.create();

		public CacheEventObserver(CacheTrace trace) {
			this.trace = trace;
		}

		@Override
		public Class<CacheEvent> getObservableClass() {
			return CacheEvent.class;
		}

		@Override
		public void onSubscription(boolean subscribed) {
			if (!subscribed) {
				topicUnsubscribed.signal();
			}
		}

		@Override
		public void topicPublished(CacheEvent event) {
			if (event.hit) {
				trace.hit = true;
			} else {
				trace.info(event.template, event.argSuppliers);
			}
		}
	}
}
