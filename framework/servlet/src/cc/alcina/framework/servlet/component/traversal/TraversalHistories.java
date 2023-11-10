package cc.alcina.framework.servlet.component.traversal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimerTask;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.servlet.LifecycleService;

@Registration.Singleton
public class TraversalHistories extends LifecycleService.AlsoDev {
	public static TraversalHistories get() {
		return Registry.impl(TraversalHistories.class);
	}

	@Override
	public void onApplicationStartup() {
		if (Configuration.is("enabled")) {
			observe();
		}
	}

	public static class TraversalHistory {
		SelectionTraversal traversal;

		long lastAccessed;

		String id;

		public TraversalHistory(SelectionTraversal traversal, String id) {
			this.traversal = traversal;
			this.id = id;
			lastAccessed = System.currentTimeMillis();
		}

		String displayName() {
			return traversal.getRootLayer().getName();
		}

		public boolean shouldEvict() {
			return id != null && !TimeConstants.within(lastAccessed,
					1 * TimeConstants.ONE_MINUTE_MS);
		}
	}

	Map<String, Topic<TraversalHistory>> histories = new LinkedHashMap<>();

	public ListenerReference subscribe(String traversalKey,
			TopicListener<TraversalHistory> subscriber) {
		return ensureTopic(traversalKey).addWithPublishedCheck(subscriber);
	}

	synchronized Topic<TraversalHistory> ensureTopic(String traversalKey) {
		return histories.computeIfAbsent(traversalKey,
				k -> Topic.create().withRetainPublished(true));
	}

	public void observe() {
		SelectionTraversal.topicTraversalComplete
				.add(this::onTraversalComplete);
		EntityLayerUtils.timer.scheduleAtFixedRate(evictTask, 0,
				TimeConstants.ONE_MINUTE_MS);
	}

	TimerTask evictTask = new TimerTask() {
		@Override
		public void run() {
			synchronized (histories) {
				histories.entrySet().stream()
						.filter(e -> e.getValue().getPublished() != null
								&& e.getValue().getPublished().shouldEvict())
						.forEach(e -> e.getValue().clearPublished());
			}
		}
	};

	void onTraversalComplete(SelectionTraversal traversal) {
		publish(null, traversal);
		publish(traversal.id, traversal);
	}

	void publish(String id, SelectionTraversal traversal) {
		ensureTopic(id).publish(new TraversalHistory(traversal, id));
	}
}
