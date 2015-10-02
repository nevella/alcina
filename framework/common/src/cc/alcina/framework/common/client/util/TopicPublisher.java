package cc.alcina.framework.common.client.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class TopicPublisher {
	private MutablePropertyChangeSupport support = new MutablePropertyChangeSupport(
			this);

	public void publishTopic(String key, Object message) {
		support.firePropertyChange(key, message == null ? "" : null, message);
	}

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

	public void removeTopicListener(String key, TopicListener listener) {
		TopicListenerAdapter adapter = lookup.get(listener, key);
		if (key == null) {
			support.removePropertyChangeListener(adapter);
		} else {
			support.removePropertyChangeListener(key, adapter);
		}
		lookup.remove(listener, key);
	}

	private static class TopicListenerAdapter<T> implements
			PropertyChangeListener {
		private final TopicListener listener;

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TopicListenerAdapter
					&& listener.equals(((TopicListenerAdapter) obj).listener);
		}

		public TopicListenerAdapter(TopicListener listener) {
			this.listener = listener;
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

	public void listenerDelta(String key, TopicListener listener, boolean add) {
		if (add) {
			addTopicListener(key, listener);
		} else {
			removeTopicListener(key, listener);
		}
	}

	public static class GlobalTopicPublisher extends TopicPublisher {
		private GlobalTopicPublisher() {
			super();
		}

		public static TopicPublisher.GlobalTopicPublisher get() {
			TopicPublisher.GlobalTopicPublisher singleton = Registry
					.checkSingleton(TopicPublisher.GlobalTopicPublisher.class);
			if (singleton == null) {
				singleton = new TopicPublisher.GlobalTopicPublisher();
				Registry.registerSingleton(
						TopicPublisher.GlobalTopicPublisher.class, singleton);
			}
			return singleton;
		}
	}

	public interface TopicListener<T> {
		void topicPublished(String key, T message);
	}
}
