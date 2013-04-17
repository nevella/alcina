package cc.alcina.framework.common.client.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

public class TopicPublisher {
	private MutablePropertyChangeSupport support = new MutablePropertyChangeSupport(
			this);

	public void publishTopic(String key, Object message) {
		support.firePropertyChange(key, message == null ? "" : null, message);
	}

	private Map<TopicListener, TopicListenerAdapter> lookup = new HashMap<TopicPublisher.TopicListener, TopicPublisher.TopicListenerAdapter>();

	public void addTopicListener(String key, TopicListener listener) {
		TopicListenerAdapter adapter = new TopicListenerAdapter(listener);
		support.addPropertyChangeListener(key, adapter);
		lookup.put(listener, adapter);
	}

	public void removeTopicListener(String key, TopicListener listener) {
		TopicListenerAdapter adapter = lookup.get(listener);
		support.removePropertyChangeListener(key, adapter);
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

		private static GlobalTopicPublisher theInstance;

		public static GlobalTopicPublisher get() {
			if (theInstance == null) {
				theInstance = new GlobalTopicPublisher();
			}
			return theInstance;
		}

		public void appShutdown() {
			theInstance = null;
		}
	}

	public interface TopicListener<T> {
		void topicPublished(String key, T message);
	}
}
