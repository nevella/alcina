package cc.alcina.framework.servlet.component.romcom.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.function.Function;

import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.logic.EntityLayerUtils;

/**
 * This class abstracts support for observable backend events which are the root
 * model for a romcom view (such as a selectiontraversal), and manages eviction
 */
public class RemoteComponentObservables<T> {
	Class<T> clazz;

	Function<T, String> observableDisplayName;

	long evictionTimeMs;

	public RemoteComponentObservables(Class<T> clazz,
			Function<T, String> observableDisplayName, long evictionTimeMs) {
		this.clazz = clazz;
		this.observableDisplayName = observableDisplayName;
		this.evictionTimeMs = evictionTimeMs;
	}

	public class ObservableHistory {
		public T observable;

		long lastAccessed;

		String id;

		public ObservableHistory(T observable, String id) {
			this.observable = observable;
			this.id = id;
			lastAccessed = System.currentTimeMillis();
		}

		public String displayName() {
			return observableDisplayName.apply(observable);
		}

		public boolean shouldEvict() {
			return id != null
					&& !TimeConstants.within(lastAccessed, evictionTimeMs);
		}
	}

	public void observe() {
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

	Map<String, Topic<ObservableHistory>> histories = new LinkedHashMap<>();

	public ListenerReference subscribe(String traversalKey,
			TopicListener<ObservableHistory> subscriber) {
		return ensureTopic(traversalKey).addWithPublishedCheck(subscriber);
	}

	synchronized Topic<ObservableHistory> ensureTopic(String traversalKey) {
		return histories.computeIfAbsent(traversalKey,
				k -> Topic.create().withRetainPublished(true));
	}

	public void publish(String id, T observable) {
		ensureTopic(id).publish(new ObservableHistory(observable, id));
	}
}
