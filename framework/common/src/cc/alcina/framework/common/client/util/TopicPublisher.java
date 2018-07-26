package cc.alcina.framework.common.client.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class TopicPublisher {
	private MutablePropertyChangeSupport support = new MutablePropertyChangeSupport(
			this);

	// listener, key - there may be multiple refs
	private UnsortedMultikeyMap<TopicListenerAdapter> lookup = new UnsortedMultikeyMap<TopicListenerAdapter>();

	public void addTopicListener(String key, TopicListener listener) {
		TopicListenerAdapter adapter = new TopicListenerAdapter(listener);
		if (key == null) {
			support.addPropertyChangeListener(adapter);
		} else {
			support.addPropertyChangeListener(key, adapter);
		}
		lookup.put(listener, key, adapter);
	}

	public void listenerDelta(String key, TopicListener listener, boolean add) {
		if (add) {
			addTopicListener(key, listener);
		} else {
			removeTopicListener(key, listener);
		}
	}

	public void publishTopic(String key, Object message) {
		support.firePropertyChange(key, message == null ? "" : null, message);
	}

	public void removeTopicListener(String key, TopicListener listener) {
		TopicListenerAdapter adapter = lookup.get(listener, key);
		if (key == null) {
			support.removePropertyChangeListener(adapter);
		} else {
			support.removePropertyChangeListener(key, adapter);
		}
		lookup.remove(listener, key);
	}

	@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
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

	public static class TopicSupport<T> {
		private String topic;
        private TopicPublisher topicPublisher;
		public TopicSupport(String topic) {
		    this(topic,true);
		}
		public TopicSupport(String topic, boolean global) {
			this.topic = topic;
			topicPublisher = global?GlobalTopicPublisher.get():new TopicPublisher();
		}
		
		public void publish(T t) {
			try {
			    topicPublisher.publishTopic(topic, t);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		public void add(TopicListener<T> listener) {
			delta(listener, true);
		}

		public void remove(TopicListener<T> listener) {
			delta(listener, false);
		}

		public void delta(TopicListener<T> listener, boolean add) {
		    topicPublisher.listenerDelta(topic, listener, add);
		}
	}

	@FunctionalInterface
	public interface TopicListener<T> {
		void topicPublished(String key, T message);
	}

	private static class TopicListenerAdapter<T>
			implements PropertyChangeListener {
		private final TopicListener listener;

		public TopicListenerAdapter(TopicListener listener) {
			this.listener = listener;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TopicListenerAdapter
					&& listener.equals(((TopicListenerAdapter) obj).listener);
		}

		@Override
		public int hashCode() {
			return listener.hashCode();
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			listener.topicPublished(evt.getPropertyName(), evt.getNewValue());
		}
	}
}
