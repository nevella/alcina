package cc.alcina.framework.common.client.process;

import java.util.Map;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;

/**
 * <p>
 * A low-consumption pub/sub registry for process events. By default (non-debug
 * mode, essentially), event publication results in just a null check
 *
 * <p>
 * Observers can either be context-based - call observe/publish methods on
 * {@code ProcessObservers.context()} (useful when modelling an isolated process
 * messages and message handling) or global - call methods on
 * {@code ProcessModel}
 *
 * 
 *
 */
public class ProcessObservers {
	/*
	 * A simple switch to turn ProcessObservers off for client optimisation
	 */
	public static boolean enabled = true;

	private static ProcessObservers instance = new ProcessObservers();

	public static ContextObservers context() {
		return ContextObservers.get();
	}

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
		if (!enabled) {
			return;
		}
		instance.publish0(observableClass, observableSupplier);
	}

	public static void publishUntyped(
			Class<? extends ProcessObservable> observableClass,
			Supplier<? extends ProcessObservable> observableSupplier) {
		if (!enabled) {
			return;
		}
		publish((Class) observableClass, (Supplier) observableSupplier);
	}

	private Map<Class<? extends ProcessObservable>, Topic> perObservableTopics;

	synchronized <O extends ProcessObservable> void observe0(
			Class<O> observableClass, TopicListener<O> listener,
			boolean register) {
		if (perObservableTopics == null) {
			perObservableTopics = (Map) CollectionCreators.Bootstrap
					.createConcurrentClassMap();
		}
		perObservableTopics
				.computeIfAbsent(observableClass, clazz -> Topic.create())
				.delta(listener, register);
	}

	<O extends ProcessObservable> void publish0(Class<O> observableClass,
			Supplier<O> observableSupplier) {
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
