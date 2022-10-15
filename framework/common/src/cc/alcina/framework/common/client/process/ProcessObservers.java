package cc.alcina.framework.common.client.process;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;

/**
 * <p>
 * A low-consumption pub/sub registry for process events. By default (non-debug
 * mode, essentially), event publication results in just a null check
 *
 * @author nick@alcina.cc
 *
 */
public class ProcessObservers {
	private static ProcessObservers instance = new ProcessObservers();

	public static <O extends ProcessObservable> void observe(
			Class<O> observableClass, TopicListener<O> listener,
			boolean register) {
		instance.observe0(observableClass, listener, register);
	}

	public static void observe(ProcessObserver o, boolean register) {
		observe(o.getObservableClass(), o, register);
	}

	public static void observe(ProcessObserver.HasObservers hasObservers) {
		hasObservers.getObservers().forEach(o -> observe(o, true));
	}

	public static <O extends ProcessObservable> void
			publish(Class<O> observableClass, Supplier<O> observableSupplier) {
		if (GWT.isScript()) {
			return;
		}
		instance.publish0(observableClass, observableSupplier);
	}

	private Map<Class<? extends ProcessObservable>, Topic> perObservableTopics;

	private synchronized <O extends ProcessObservable> void observe0(
			Class<O> observableClass, TopicListener<O> listener,
			boolean register) {
		if (perObservableTopics == null) {
			synchronized (this) {
				if (perObservableTopics == null) {
					perObservableTopics = new ConcurrentHashMap<>();
				}
			}
		}
		perObservableTopics
				.computeIfAbsent(observableClass, clazz -> Topic.create())
				.delta(listener, register);
	}

	private <O extends ProcessObservable> void
			publish0(Class<O> observableClass, Supplier<O> observableSupplier) {
		if (perObservableTopics == null) {
			return;
		}
		Topic topic = perObservableTopics.get(observableClass);
		if (topic == null) {
			return;
		}
		topic.publish(observableSupplier.get());
	}
}
