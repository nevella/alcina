package cc.alcina.framework.servlet.component.romcom.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimerTask;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.servlet.environment.EnvironmentManager;
import cc.alcina.framework.servlet.environment.EnvironmentManager.EnvironmentSource;

/**
 * This class abstracts support for observable backend events which are the root
 * model for a romcom view (such as a selectiontraversal), and manages eviction
 */
public class RemoteComponentObservables<T> {
	public static final transient String CONTEXT_OVERRIDE_EVICTION_TIME = RemoteComponentObservables.class
			.getName() + ".CONTEXT_OVERRIDE_EVICTION_TIME";

	Class<? extends RemoteComponent> componentClass;

	Class<T> observedClass;

	Function<T, String> observableDisplayName;

	long evictionTimeMs;

	Logger logger = LoggerFactory.getLogger(getClass());

	public String path;

	TimerTask evictTask = new TimerTask() {
		@Override
		public void run() {
			checkEvict(false);
		}
	};

	void checkEvict(boolean log) {
		synchronized (keyObservable) {
			keyObservable.entrySet().stream()
					.filter(e -> e.getValue().getPublished() != null
							&& e.getValue().getPublished().shouldEvict(log))
					.forEach(RemoteComponentObservables.this::evict);
		}
	}

	void evict(Map.Entry<String, Topic<ObservableEntry>> entry) {
		ObservableEntry published = entry.getValue().getPublished();
		entry.getValue().clearPublished();
		logger.info("evicted-ObservableHistory - key: [{}]", entry.getKey());
		if (published != null) {
			EnvironmentManager.get()
					.deregisterSource(published.toEnvironmentSource());
		}
	}

	Map<String, Topic<ObservableEntry>> keyObservable = new LinkedHashMap<>();

	public void evict(String key) {
		synchronized (keyObservable) {
			keyObservable.entrySet().stream()
					.filter(e -> Objects.equals(e.getKey(), key))
					.forEach(this::evict);
		}
	}

	public RemoteComponentObservables(
			Class<? extends RemoteComponent> componentClass,
			Class<T> observedClass, Function<T, String> observableDisplayName,
			long evictionTimeMs) {
		this.componentClass = componentClass;
		this.path = Reflections.newInstance(componentClass).getPath();
		this.observedClass = observedClass;
		this.observableDisplayName = observableDisplayName;
		this.evictionTimeMs = evictionTimeMs;
	}

	synchronized Topic<ObservableEntry> ensureTopic(String traversalKey) {
		return keyObservable.computeIfAbsent(traversalKey,
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
		ObservableEntry history = new ObservableEntry(observable, id);
		EnvironmentManager.get().registerSource(history.toEnvironmentSource());
		ensureTopic(id).publish(history);
	}

	public ListenerReference subscribe(String traversalKey,
			TopicListener<ObservableEntry> subscriber) {
		return ensureTopic(traversalKey).addWithPublishedCheck(subscriber);
	}

	public class ObservableEntry {
		public static final Configuration.Key evictionDisabled = Configuration
				.key("evictionDisabled");

		public static final Configuration.Key evictNullId = Configuration
				.key("evictNullId");

		T observable;

		public T getObservable() {
			onObserved();
			return observable;
		}

		long lastAccessed;

		String id;

		long observableEvictionTimeMs;

		@Override
		public String toString() {
			return Ax.format("%s :: %s", getClass().getSimpleName(),
					NestedName.get(observable));
		}

		void onObserved() {
			lastAccessed = System.currentTimeMillis();
		}

		public ObservableEntry(T observable, String id) {
			this.observable = observable;
			this.id = id;
			lastAccessed = System.currentTimeMillis();
			onObserved();
			Long overrideEvictionTime = LooseContext
					.getLong(CONTEXT_OVERRIDE_EVICTION_TIME);
			if (overrideEvictionTime != null) {
				observableEvictionTimeMs = overrideEvictionTime;
				if (overrideEvictionTime > evictionTimeMs) {
					// no need to warn if the eviction is _shorter_ than default
					logger.warn(
							"Override eviction - Observed component observable: {} {}",
							observable.getClass().getName(), id);
				}
			}
		}

		public EnvironmentSource toEnvironmentSource() {
			return new EnvironmentSource(getPath(), getHref());
		}

		String getPath() {
			return Ax.format("%s/%s", path, id);
		}

		String getHref() {
			return Ax.format("%s?path=%s/%s", path, path, id);
		}

		public String displayName() {
			return observableDisplayName.apply(observable);
		}

		public boolean shouldEvict(boolean log) {
			if (log) {
				logger.info(
						"Check eviction - id: {} - lastAccessed: {} - observableEvictionTimeMs: {}",
						id, lastAccessed, observableEvictionTimeMs);
			}
			if (evictionDisabled.is()) {
				return false;
			}
			return (id != null || evictNullId.is()) && !TimeConstants
					.within(lastAccessed, observableEvictionTimeMs);
		}
	}

	public static void setOverrideEvictionTime(long overrideEvictionTimeMs) {
		LooseContext.set(CONTEXT_OVERRIDE_EVICTION_TIME,
				overrideEvictionTimeMs);
	}

	public void observed(String key) {
		synchronized (keyObservable) {
			keyObservable.entrySet().stream()
					.filter(e -> Objects.equals(e.getKey(), key))
					.map(e -> e.getValue().getPublished())
					.filter(Objects::nonNull)
					.forEach(ObservableEntry::onObserved);
		}
	}
}
