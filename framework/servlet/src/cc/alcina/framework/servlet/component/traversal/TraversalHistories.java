package cc.alcina.framework.servlet.component.traversal;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
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

		public TraversalHistory(SelectionTraversal traversal) {
			this.traversal = traversal;
		}

		String displayName() {
			return traversal.getRootLayer().getName();
		}
	}

	Map<String, Topic<TraversalHistory>> histories = new LinkedHashMap<>();

	public ListenerReference subscribe(String traversalKey,
			TopicListener<TraversalHistory> subscriber) {
		synchronized (histories) {
			return histories
					.computeIfAbsent(traversalKey,
							k -> Topic.create().withRetainPublished(true))
					.addWithPublishedCheck(subscriber);
		}
	}

	public void observe() {
		SelectionTraversal.topicTraversalComplete
				.add(this::onTraversalComplete);
	}

	void onTraversalComplete(SelectionTraversal traversal) {
		publishIfKey(null, traversal);
		publishIfKey(traversal.id, traversal);
	}

	void publishIfKey(String id, SelectionTraversal traversal) {
		Topic<TraversalHistory> topic;
		synchronized (histories) {
			topic = histories.get(id);
		}
		if (topic != null) {
			topic.publish(new TraversalHistory(traversal));
		}
	}
}
