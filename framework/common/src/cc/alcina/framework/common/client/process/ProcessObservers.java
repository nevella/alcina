package cc.alcina.framework.common.client.process;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.common.client.util.LooseContextInstance.Frame;
import cc.alcina.framework.common.client.util.Multimap;
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
		if (GWT.isScript()) {
			return;
		}
		instance.publish0(observableClass, observableSupplier);
	}

	public static void publishUntyped(
			Class<? extends ProcessObservable> observableClass,
			Supplier<? extends ProcessObservable> observableSupplier) {
		if (GWT.isScript()) {
			return;
		}
		publish((Class) observableClass, (Supplier) observableSupplier);
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
	 *
	 * Thread-safe - due to the co
	 */
	public static class ContextObservers {
		public static boolean has() {
			return LooseContext.has(key());
		}

		static ContextObservers get() {
			return has() ? LooseContext.get(key()) : new ContextObservers();
		}

		static String key() {
			return ContextObservers.class.getName();
		}

		private ProcessObservers instance = new ProcessObservers();

		private Multimap<LooseContextInstance.Frame, List<ProcessObserver>> observers = new Multimap<>();

		private ContextObservers() {
		}

		// this is a relatively complex use of context, since listeners can be
		// added to the (one) ContextObservers at various depths. So validate on
		// publish.
		public void observe(ProcessObserver o) {
			// only ensure a ContextObservers exists in the context here
			if (!has()) {
				LooseContext.set(key(), this);
			}
			instance.observe0(o.getObservableClass(), o, true);
			LooseContextInstance.Frame frame = LooseContext.getContext()
					.getFrame();
			observers.add(frame, o);
		}

		public <O extends ProcessObservable> void publish(O observable) {
			if (observers.size() > 0) {
				// validate observer frames
				Iterator<Entry<Frame, List<ProcessObserver>>> itr = observers
						.entrySet().iterator();
				while (itr.hasNext()) {
					Entry<Frame, List<ProcessObserver>> entry = itr.next();
					LooseContextInstance.Frame frame = entry.getKey();
					if (!frame.isActive()) {
						entry.getValue().forEach(o -> instance
								.observe0(o.getObservableClass(), o, false));
						itr.remove();
					}
				}
			}
			instance.publish0((Class<O>) observable.getClass(),
					() -> observable);
		}
	}
}
