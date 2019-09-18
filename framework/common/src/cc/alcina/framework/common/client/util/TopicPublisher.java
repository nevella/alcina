package cc.alcina.framework.common.client.util;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*
 * Key 'null' (when passed to addTopicListener) receives all topics
 */
public class TopicPublisher {
	// use a list - the listener may be added/removed multiple times (although
	// that's probably not what's wanted)
	private Multimap<String, List<TopicListener>> lookup = new Multimap<>();

	public void addTopicListener(String key, TopicListener listener) {
		synchronized (lookup) {
			lookup.add(key, listener);
		}
	}

	public void listenerDelta(String key, TopicListener listener, boolean add) {
		if (add) {
			addTopicListener(key, listener);
		} else {
			removeTopicListener(key, listener);
		}
	}

	public void publishTopic(String key, Object message) {
		List<TopicListener> listeners = null;
		synchronized (lookup) {
			listeners = lookup.getAndEnsure(key).stream()
					.collect(Collectors.toList());
			lookup.getAndEnsure(null).stream().forEach(listeners::add);
		}
		for (TopicListener listener : listeners) {
			listener.topicPublished(key, message);
		}
	}

	public void removeTopicListener(String key, TopicListener listener) {
		synchronized (lookup) {
			lookup.subtract(key, listener);
		}
	}

	@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
	public static class GlobalTopicPublisher extends TopicPublisher {
		private static volatile GlobalTopicPublisher singleton;

		public static GlobalTopicPublisher get() {
			if (singleton == null) {
				synchronized (GlobalTopicPublisher.class) {
					singleton = new GlobalTopicPublisher();
					Registry.registerSingleton(GlobalTopicPublisher.class,
							singleton);
				}
			}
			return singleton;
		}

		private GlobalTopicPublisher() {
			super();
		}
	}

	@FunctionalInterface
	public interface TopicListener<T> {
		void topicPublished(String key, T message);
	}

	public static class TopicSupport<T> {
		private String topic;

		private TopicPublisher topicPublisher;

		private boolean wasPublished;

		public TopicSupport(String topic) {
			this(topic, true);
		}

		public TopicSupport(String topic, boolean global) {
			this.topic = topic;
			topicPublisher = global ? GlobalTopicPublisher.get()
					: new TopicPublisher();
		}

		public void add(TopicListener<T> listener) {
			add(listener, false);
		}

		public void add(TopicListener<T> listener, boolean fireIfWasPublished) {
			delta(listener, true);
			if (wasPublished && fireIfWasPublished) {
				// note - we don't keep a ref to the last published object -
				// this assumes the caller knows how to get it. Useful for
				// adding async one-off listeners when the event may have
				// already occurred
				listener.topicPublished(topic, null);
			}
		}

		public void addRunnable(Runnable runnable) {
			addRunnable(runnable, false);
		}

		public void addRunnable(Runnable runnable, boolean fireIfWasPublished) {
			add(new TopicListener() {
				@Override
				public void topicPublished(String key, Object message) {
					runnable.run();
				}
			}, fireIfWasPublished);
		}

		public void delta(TopicListener<T> listener, boolean add) {
			topicPublisher.listenerDelta(topic, listener, add);
		}

		public void publish(T t) {
			try {
				topicPublisher.publishTopic(topic, t);
				wasPublished = true;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		public void remove(TopicListener<T> listener) {
			delta(listener, false);
		}
	}
}
