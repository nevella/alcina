package cc.alcina.framework.common.client.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;

public class TopicPublisher {
	private MutablePropertyChangeSupport support = new MutablePropertyChangeSupport(
			this);

	public void publish(String key, Object message) {
		support.firePropertyChange(key, null, message);
	}

	private Map<TopicListener, TopicListenerAdapter> lookup = new HashMap<TopicPublisher.TopicListener, TopicPublisher.TopicListenerAdapter>();

	public void addListener(String key,TopicListener listener) {
		TopicListenerAdapter adapter = new TopicListenerAdapter(listener);
		support.addPropertyChangeListener(key,adapter);
		lookup.put(listener,adapter);
	}
	public void removeListener(String key,TopicListener listener) {
		TopicListenerAdapter adapter = lookup.get(listener);
		support.removePropertyChangeListener(key,adapter);
	}

	private static class TopicListenerAdapter<T> implements PropertyChangeListener {
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

	public interface TopicListener<T> {
		void topicPublished(String key, T message);
	}
}
