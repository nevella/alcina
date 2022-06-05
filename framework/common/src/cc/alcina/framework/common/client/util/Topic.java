package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;

/*
 * A message passing mechanism. Previously topics also had a string key -
 * this was abandoned, instead use multiple topics. Topics are useful both
 * as static class members (app-level messages) and instance members, and
 * are generally public fields (since they're initialised on class
 * instantiation and generally final)
 */
public class Topic<T> {
	public static <T> Topic<T> create() {
		return new Topic<>();
	}

	private Publisher publisher;

	private boolean wasPublished;

	private boolean throwExceptions = false;

	private Topic() {
		publisher = new Publisher();
	}

	public TopicListener.Reference add(TopicListener<T> listener) {
		return add(listener, false);
	}

	public TopicListener.Reference add(TopicListener<T> listener,
			boolean fireIfWasPublished) {
		delta(listener, true);
		if (wasPublished && fireIfWasPublished) {
			// note - we don't keep a ref to the last published object -
			// this assumes the caller knows how to get it. Useful for
			// adding async one-off listeners when the event may have
			// already occurred
			listener.topicPublished(null);
		}
		return new TopicListener.Reference(this, listener);
	}

	public void addRunnable(Runnable runnable) {
		addRunnable(runnable, false);
	}

	public void addRunnable(Runnable runnable, boolean fireIfWasPublished) {
		add(new TopicListener() {
			@Override
			public void topicPublished(Object message) {
				runnable.run();
			}
		}, fireIfWasPublished);
	}

	public void clearListeners() {
		publisher.clearListeners();
	}

	public void delta(TopicListener<T> listener, boolean add) {
		publisher.listenerDelta(listener, add);
	}

	public boolean hasListeners() {
		return publisher.hasListeners();
	}

	// FIXME - 2022 - remove try/catch
	public void publish(T t) {
		try {
			publisher.publishTopic(t);
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

	public void signal() {
		publish(null);
	}

	public <S> Topic<S> withThrowExceptions() {
		throwExceptions = true;
		return (Topic<S>) this;
	}

	public static class MultichannelTopics<TC> {
		private TC firingChannel;

		private Map<TC, Topic> byChannel = new LinkedHashMap<>();

		public TC getFiringChannel() {
			return this.firingChannel;
		}

		public synchronized void listenerDelta(TC channel,
				TopicListener listener, boolean add) {
			ensureTopic(channel).delta(listener, add);
		}

		public synchronized void publish(TC channel, Object object) {
			try {
				firingChannel = channel;
				ensureTopic(channel).publish(object);
			} finally {
				firingChannel = null;
			}
		}

		private Topic ensureTopic(TC channel) {
			return byChannel.computeIfAbsent(channel, c -> Topic.create());
		}
	}

	/*
	 *
	 * Thread-safe (accesses to lookup are synchronized - defensive copy made
	 * for publishTopic)
	 */
	public static class Publisher {
		// use a list - the listener may be added/removed multiple times
		// (although
		// that's probably not what's wanted)
		private List<TopicListener> lookup = new ArrayList<>();

		public void addListener(TopicListener listener) {
			synchronized (lookup) {
				lookup.add(listener);
			}
		}

		public void clearListeners() {
			Preconditions.checkState(GWT.isClient());
			lookup.clear();
		}

		public boolean hasListeners() {
			synchronized (lookup) {
				return lookup.size() > 0;
			}
		}

		public void listenerDelta(TopicListener listener, boolean add) {
			if (add) {
				addListener(listener);
			} else {
				removeListener(listener);
			}
		}

		public void publishTopic(Object message) {
			List<TopicListener> listeners = null;
			synchronized (lookup) {
				listeners = lookup.stream().collect(Collectors.toList());
			}
			for (TopicListener listener : listeners) {
				listener.topicPublished(message);
			}
		}

		public void removeListener(TopicListener listener) {
			synchronized (lookup) {
				lookup.remove(listener);
			}
		}
	}
}