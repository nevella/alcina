package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

/*
 * A message passing mechanism. Previously topics also had a string key - this
 * was abandoned, instead use multiple topics. Topics are useful both as static
 * class members (app-level messages) and instance members, and are generally
 * public fields (since they're initialised on class instantiation and generally
 * final)
 */
public class Topic<T> {
	public static <T> Topic<T> create() {
		return new Topic<>();
	}

	protected Publisher publisher;

	private boolean wasPublished;

	private boolean retainPublished;

	private T published;

	/**
	 * This class (unlike topic) only supports one listener at a time -
	 * otherwise retention of multiple events is overly complex (for which
	 * listener?), and assumes single-threaded access
	 */
	public static class RetainMultiple<T> extends Topic<T> {
		List<T> retained = new ArrayList<>();

		public static <T> RetainMultiple<T> create() {
			RetainMultiple<T> retainMultiple = new RetainMultiple<>();
			retainMultiple.withRetainPublished(true);
			return retainMultiple;
		}

		@Override
		public void publish(T t) {
			super.publish(t);
			retained.add(t);
		}

		@Override
		protected ListenerReference add(TopicListener<T> listener,
				boolean fireIfWasPublished) {
			Preconditions.checkState(
					!fireIfWasPublished || !publisher.hasListeners());
			return super.add(listener, fireIfWasPublished);
		}

		@Override
		protected void fireOnAddWasPublished(TopicListener<T> listener) {
			retained.forEach(listener::topicPublished);
		}
	}

	protected Topic() {
		publisher = new Publisher();
	}

	public ListenerReference add(Runnable runnable) {
		return add(runnable, false);
	}

	private ListenerReference add(Runnable runnable,
			boolean fireIfWasPublished) {
		return add(new TopicListener() {
			@Override
			public void topicPublished(Object message) {
				runnable.run();
			}
		}, fireIfWasPublished);
	}

	public ListenerReference add(TopicListener<T> listener) {
		return add(listener, false);
	}

	protected ListenerReference add(TopicListener<T> listener,
			boolean fireIfWasPublished) {
		delta(listener, true);
		if (wasPublished && fireIfWasPublished) {
			/*
			 * note - unless retainpublished is set, we don't keep a ref to the
			 * last published object - this assumes the caller knows how to get
			 * it. Useful for adding async one-off listeners when the event may
			 * have already occurred
			 */
			fireOnAddWasPublished(listener);
		}
		return new Topic.Reference(this, listener);
	}

	protected void fireOnAddWasPublished(TopicListener<T> listener) {
		listener.topicPublished(published);
	}

	public ListenerReference addWithPublishedCheck(Runnable runnable) {
		return add(runnable, true);
	}

	public ListenerReference addWithPublishedCheck(TopicListener<T> listener) {
		return add(listener, true);
	}

	public void clearListeners() {
		publisher.clearListeners();
	}

	public void clearPublished() {
		wasPublished = false;
		published = null;
	}

	public void delta(TopicListener<T> listener, boolean add) {
		publisher.listenerDelta(listener, add);
	}

	public void fireIfPublished(Consumer<T> consumer) {
		if (wasPublished) {
			fireOnAddWasPublished(consumer::accept);
		}
	}

	public T getPublished() {
		return published;
	}

	public boolean hasListeners() {
		return publisher.hasListeners();
	}

	public void publish(T t) {
		publisher.publishTopic(t);
		wasPublished = true;
		if (retainPublished) {
			published = t;
		}
	}

	public void remove(TopicListener<T> listener) {
		delta(listener, false);
	}

	public void signal() {
		publish(null);
	}

	public <V> Topic<V> withRetainPublished(boolean retainPublished) {
		this.retainPublished = retainPublished;
		return (Topic<V>) this;
	}

	public static class MultichannelTopics<TC> {
		private TC firingChannel;

		private Map<TC, Topic> byChannel = new LinkedHashMap<>();

		private Topic ensureTopic(TC channel) {
			return byChannel.computeIfAbsent(channel, c -> Topic.create());
		}

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
	}

	/*
	 *
	 * Thread-safe (lookup is copy-on-write, mutation is synchronized, all
	 * readers of lookup get a single instance on method start)
	 */
	public static class Publisher {
		/*
		 * use a list - the listener may be added/removed multiple times
		 * (although that's probably not what's wanted)
		 */
		private volatile List<TopicListener> listeners;

		public void addListener(TopicListener listener) {
			synchronized (this) {
				List<TopicListener> list = listeners == null ? new ArrayList<>()
						: new ArrayList<>(listeners);
				list.add(listener);
				if (listener instanceof TopicListener.HandlesSubscription) {
					((TopicListener.HandlesSubscription) listener)
							.onSubscription(true);
				}
				listeners = list;
			}
		}

		public void clearListeners() {
			Preconditions.checkState(GWT.isClient());
			synchronized (this) {
				listeners = null;
			}
		}

		public boolean hasListeners() {
			List<TopicListener> lookup = this.listeners;
			return lookup != null && lookup.size() > 0;
		}

		public void listenerDelta(TopicListener listener, boolean add) {
			if (add) {
				addListener(listener);
			} else {
				removeListener(listener);
			}
		}

		public void publishTopic(Object message) {
			List<TopicListener> lookup = this.listeners;
			if (lookup == null) {
				return;
			}
			for (TopicListener listener : lookup) {
				listener.topicPublished(message);
			}
		}

		public void removeListener(TopicListener listener) {
			synchronized (this) {
				if (listeners == null) {
					return;
				}
				List<TopicListener> list = new ArrayList<>(listeners);
				boolean removed = list.remove(listener);
				if (removed) {
					if (listener instanceof TopicListener.HandlesSubscription) {
						((TopicListener.HandlesSubscription) listener)
								.onSubscription(false);
					}
				}
				listeners = list;
			}
		}
	}

	static class Reference implements ListenerReference {
		private TopicListener listener;

		private Topic topic;

		public Reference(Topic topic, TopicListener listener) {
			this.topic = topic;
			this.listener = listener;
		}

		@Override
		public void remove() {
			topic.remove(listener);
		}

		@Override
		public void removeOnFire() {
			topic.remove(listener);
			TopicListener wrapper = new TopicListener() {
				@Override
				public void topicPublished(Object o) {
					listener.topicPublished(o);
					topic.remove(this);
				}
			};
			topic.add(wrapper);
		}
	}
}