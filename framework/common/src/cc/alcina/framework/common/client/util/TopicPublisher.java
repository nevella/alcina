package cc.alcina.framework.common.client.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*
 * Key 'null' (when passed to addTopicListener) receives all topics
 *
 * Thread-safe (accesses to lookup are synchronized - defensive copy made for publishTopic)
 */
public class TopicPublisher {
	// use a list - the listener may be added/removed multiple times (although
	// that's probably not what's wanted)
	private Multimap<String, List<TopicListener>> lookup = new Multimap<>();

	private Set<TopicListener> removeOnFire = null;

	public void addTopicListener(String key, TopicListener listener) {
		synchronized (lookup) {
			lookup.add(key, listener);
		}
	}

	public void clearListeners(String topic) {
		Preconditions.checkState(GWT.isClient());
		lookup.getAndEnsure(topic).clear();
	}

	public boolean hasListeners(String key) {
		synchronized (lookup) {
			return lookup.containsKey(key) && lookup.get(key).size() > 0;
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
			if (key != null) {
				lookup.getAndEnsure(null).stream().forEach(listeners::add);
			}
		}
		Set<TopicListener> removeOnFire = this.removeOnFire;
		for (TopicListener listener : listeners) {
			if (removeOnFire != null) {
				Preconditions.checkArgument(key == null);
				if (removeOnFire.remove(listener)) {
					synchronized (lookup) {
						lookup.get(null).remove(listener);
					}
				}
			}
			listener.topicPublished(key, message);
		}
	}

	public void removeTopicListener(String key, TopicListener listener) {
		synchronized (lookup) {
			lookup.subtract(key, listener);
		}
	}

	private synchronized Set<TopicListener> ensureRemoveOnFire() {
		if (removeOnFire == null) {
			removeOnFire = Collections.synchronizedSet(new LinkedHashSet<>());
		}
		return removeOnFire;
	}

	@Registration(ClearStaticFieldsOnAppShutdown.class)
	public static class GlobalTopicPublisher extends TopicPublisher {
		private static volatile GlobalTopicPublisher singleton;

		public static GlobalTopicPublisher get() {
			if (singleton == null) {
				synchronized (GlobalTopicPublisher.class) {
					singleton = new GlobalTopicPublisher();
					Registry.register().singleton(GlobalTopicPublisher.class,
							singleton);
				}
			}
			return singleton;
		}
	}

	/*
	 * Global topics are not tied to a particular instance of topic - they're
	 * application-global message publication points.
	 *
	 * In hindsight, they may be an unnecessary abstraction - since if the topic
	 * key (string) is visible, generally the API (i.e. the published object
	 * class) of the topic is as well. That is to say that possibly 'local'
	 * topics (implemented as static topic fields on the message container
	 * classs) are probably all we need.
	 */
	public static class Topic<T> {
		public static <T> Topic<T> global(String topic) {
			Objects.requireNonNull(topic);
			return new Topic<>(topic, true);
		}

		public static <T> Topic<T> local() {
			return new Topic<>(null, false);
		}

		private String topic;

		private TopicPublisher topicPublisher;

		private boolean wasPublished;

		private boolean throwExceptions = false;

		private Topic(String topic, boolean global) {
			this.topic = topic;
			topicPublisher = global ? GlobalTopicPublisher.get()
					: new TopicPublisher();
		}

		public TopicListenerReference add(TopicListener<T> listener) {
			return add(listener, false);
		}

		public TopicListenerReference add(TopicListener<T> listener,
				boolean fireIfWasPublished) {
			delta(listener, true);
			if (wasPublished && fireIfWasPublished) {
				// note - we don't keep a ref to the last published object -
				// this assumes the caller knows how to get it. Useful for
				// adding async one-off listeners when the event may have
				// already occurred
				listener.topicPublished(topic, null);
			}
			return new TopicListenerReference(this, listener);
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

		public void clearListeners() {
			topicPublisher.clearListeners(topic);
		}

		public void delta(TopicListener<T> listener, boolean add) {
			topicPublisher.listenerDelta(topic, listener, add);
		}

		public boolean hasListeners() {
			return topicPublisher.hasListeners(topic);
		}

		public void publish() {
			publish(null);
		}

		// FIXME - 2021 - remove try/catch
		public void publish(T t) {
			try {
				topicPublisher.publishTopic(topic, t);
				wasPublished = true;
			} catch (Throwable e) {
				if (throwExceptions) {
					throw WrappedRuntimeException.wrap(e);
				} else {
					e.printStackTrace();
				}
			}
		}

		public void remove(TopicListener<T> listener) {
			delta(listener, false);
		}

		public void removeOnFire(TopicListener listener) {
			topicPublisher.ensureRemoveOnFire().add(listener);
		}

		public <S> Topic<S> withThrowExceptions() {
			throwExceptions = true;
			return (Topic<S>) this;
		}
	}

	@FunctionalInterface
	public interface TopicListener<T> {
		void topicPublished(String key, T message);
	}

	public static class TopicListenerReference {
		private TopicListener listener;

		private Topic topic;

		private boolean removeOnFire;

		public TopicListenerReference(Topic topic, TopicListener listener) {
			this.topic = topic;
			this.listener = listener;
		}

		public void remove() {
			topic.remove(listener);
		}

		public void removeOnFire() {
			topic.removeOnFire(listener);
		}
	}
}
