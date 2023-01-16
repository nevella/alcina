package cc.alcina.framework.common.client.process;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.util.LooseContext;
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
 * @author nick@alcina.cc
 *
 */
public class ProcessObservers {
	private static ProcessObservers instance = new ProcessObservers();

	public static ContextObservers context() {
		return ContextObservers.has() ? LooseContext.get(ContextObservers.key())
				: new ContextObservers();
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
			perObservableTopics = new ConcurrentHashMap<>();
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

	/*
	 * Higher-cost than process observers, since the caller (if passing control)
	 * will generally maintain a multable state that allows ancestor
	 * modification.
	 *
	 * Used for context-based control, as well as observation of a process. See
	 * e.g. TreeSync.Preparer
	 */
	public static class ContextObservers {
		public static boolean has() {
			return LooseContext.has(key());
		}

		public static String key() {
			return ContextObservers.class.getName();
		}

		private ProcessObservers instance = new ProcessObservers();

		ContextObservers() {
		}

		// deregistration is handled by the context going out of scope
		public void observe(ProcessObserver o) {
			if (!has()) {
				LooseContext.set(key(), this);
			}
			instance.observe0(o.getObservableClass(), o, true);
		}

		public <O extends ProcessObservable> void publish(O observable) {
			instance.publish0((Class<O>) observable.getClass(),
					() -> observable);
		}
	}
}
