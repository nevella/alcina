package cc.alcina.framework.servlet.component.romcom.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.logic.EntityLayerUtils;

/**
 * This class abstracts support for observable backend events which are the root
 * model for a romcom view (such as a selectiontraversal), and manages eviction
 */
public class RemoteComponentObservables<T> {
	public static final transient String CONTEXT_OVERRIDE_EVICTION_TIME = RemoteComponentObservables.class
			.getName() + ".CONTEXT_OVERRIDE_EVICTION_TIME";

	Class<T> clazz;

	Function<T, String> observableDisplayName;

	long evictionTimeMs;

	Logger logger = LoggerFactory.getLogger(getClass());

	public String path;

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

	public RemoteComponentObservables(Class<T> clazz,
			Function<T, String> observableDisplayName, long evictionTimeMs) {
		this.clazz = clazz;
		this.observableDisplayName = observableDisplayName;
		this.evictionTimeMs = evictionTimeMs;
	}

	synchronized Topic<ObservableHistory> ensureTopic(String traversalKey) {
		return histories.computeIfAbsent(traversalKey,
				k -> Topic.create().withRetainPublished(true));
	}

	public void observe() {
		EntityLayerUtils.timer.scheduleAtFixedRate(evictTask, 0,
				TimeConstants.ONE_MINUTE_MS);
	}

	public void publish(String id, T observable) {
		if (Configuration.is(RemoteComponentObservables.class,
				"onlyRetainOverridenEviction")) {
			if (!LooseContext.has(CONTEXT_OVERRIDE_EVICTION_TIME)) {
				return;
			}
		}
		ensureTopic(id).publish(new ObservableHistory(observable, id));
	}

	public ListenerReference subscribe(String traversalKey,
			TopicListener<ObservableHistory> subscriber) {
		return ensureTopic(traversalKey).addWithPublishedCheck(subscriber);
	}

	public class ObservableHistory {
		public T observable;

		long lastAccessed;

		String id;

		long observableEvictionTimeMs;

		public ObservableHistory(T observable, String id) {
			this.observable = observable;
			this.id = id;
			lastAccessed = System.currentTimeMillis();
			observableEvictionTimeMs = evictionTimeMs;
			Long overrideEvictionTime = LooseContext
					.getLong(CONTEXT_OVERRIDE_EVICTION_TIME);
			if (overrideEvictionTime != null) {
				observableEvictionTimeMs = overrideEvictionTime;
				logger.warn("Observed component observable: {} {}",
						observable.getClass().getName(), id);
			}
		}

		public String displayName() {
			return observableDisplayName.apply(observable);
		}

		public boolean shouldEvict() {
			return id != null && !TimeConstants.within(lastAccessed,
					observableEvictionTimeMs);
		}
	}

	public static void setOverrideEvictionTime(long overrideEvictionTimeMs) {
		LooseContext.set(CONTEXT_OVERRIDE_EVICTION_TIME,
				overrideEvictionTimeMs);
	}
}
