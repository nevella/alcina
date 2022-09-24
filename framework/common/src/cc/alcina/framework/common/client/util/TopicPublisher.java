package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

/*
 *
 * Thread-safe (accesses to lookup are synchronized - defensive copy made for publishTopic)
 */
public class TopicPublisher {
	// use a list - the listener may be added/removed multiple times (although
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
