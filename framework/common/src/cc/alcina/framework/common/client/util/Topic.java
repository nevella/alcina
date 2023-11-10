package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	private Publisher publisher;

	private boolean wasPublished;

	private boolean retainPublished;

	public <V> Topic<V> withRetainPublished(boolean retainPublished) {
		this.retainPublished = retainPublished;
		return (Topic<V>) this;
	}

	private T published;

	private Topic() {
		publisher = new Publisher();
	}

	public ListenerReference add(Runnable runnable) {
		return add(runnable, false);
	}

	public ListenerReference add(TopicListener<T> listener) {
		return add(listener, false);
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

	public void delta(TopicListener<T> listener, boolean add) {
		publisher.listenerDelta(listener, add);
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

	private ListenerReference add(Runnable runnable,
			boolean fireIfWasPublished) {
		return add(new TopicListener() {
			@Override
			public void topicPublished(Object message) {
				runnable.run();
			}
		}, fireIfWasPublished);
	}

	private ListenerReference add(TopicListener<T> listener,
			boolean fireIfWasPublished) {
		delta(listener, true);
		if (wasPublished && fireIfWasPublished) {
			/*
			 * note - unless retainpublished is set, we don't keep a ref to the
			 * last published object - this assumes the caller knows how to get
			 * it. Useful for adding async one-off listeners when the event may
			 * have already occurred
			 */
			listener.topicPublished(published);
		}
		return new Topic.Reference(this, listener);
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
	 * Thread-safe (lookup is copy-on-write, mutation is synchronized)
	 */
	public static class Publisher {
		/*
		 * use a list - the listener may be added/removed multiple times
		 * (although that's probably not what's wanted)
		 */
		private List<TopicListener> lookup;

		public void addListener(TopicListener listener) {
			synchronized (this) {
				List<TopicListener> list = lookup == null ? new ArrayList<>()
						: new ArrayList<>(lookup);
				list.add(listener);
				if (listener instanceof TopicListener.HandlesSubscription) {
					((TopicListener.HandlesSubscription) listener)
							.onSubscription(true);
				}
				lookup = list;
			}
		}

		public void clearListeners() {
			Preconditions.checkState(GWT.isClient());
			lookup = null;
		}

		public boolean hasListeners() {
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
			if (lookup == null) {
				return;
			}
			for (TopicListener listener : lookup) {
				listener.topicPublished(message);
			}
		}

		public void removeListener(TopicListener listener) {
			synchronized (this) {
				if (lookup == null) {
					return;
				}
				List<TopicListener> list = new ArrayList<>(lookup);
				boolean removed = list.remove(listener);
				if (removed) {
					if (listener instanceof TopicListener.HandlesSubscription) {
						((TopicListener.HandlesSubscription) listener)
								.onSubscription(false);
					}
				}
				lookup = list;
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