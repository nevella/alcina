package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.ListenerBinding;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.model.Model.Bindings;
import cc.alcina.framework.gwt.client.util.HasBind;

/**
 *
 * Build a binding pipeline with a lifecycle controlled by the registering model
 *
 * @param <T>
 */
public class ModelBinding<T> {
	Bindings bindings;

	SourcesPropertyChangeEvents fromPropertyChangeSource;

	Object on;

	Function<?, ?> map;

	Supplier<?> supplier;

	Consumer<?> consumer;

	ListenerBinding listener;

	boolean setOnInitialise = true;

	Ref<Consumer<Runnable>> dispatchRef = null;

	Predicate<T> predicate;

	boolean transformsNull;

	Predicate<T> preMapPredicate;

	Topic<?> fromTopic;

	public ModelBinding<T> withTransformsNull() {
		this.transformsNull = true;
		return this;
	}

	public ModelBinding(Bindings bindings) {
		this.bindings = bindings;
	}

	/**
	 * add a terminal consumer (i.e. the actual action performer) to the end of
	 * the pipeline
	 */
	public void accept(Consumer<T> consumer) {
		this.consumer = consumer;
	}

	/**
	 * When the change occurs, rather than pipe the event/change, pipe the
	 * object from <code>supplier</code>
	 */
	public <V> ModelBinding<V> value(Supplier<V> supplier) {
		this.supplier = supplier;
		return (ModelBinding<V>) this;
	}

	/**
	 * When the change occurs, rather than pipe the event/change, pipe the
	 * replaceWith object
	 */
	public <V> ModelBinding<V> value(V replaceWith) {
		return value(() -> replaceWith);
	}

	/**
	 * The source of the binding property changes
	 */
	public ModelBinding<T> from(SourcesPropertyChangeEvents from) {
		this.fromPropertyChangeSource = from;
		return this;
	}

	/**
	 * Add an intermediate mapping to the pipeline
	 */
	public <U> ModelBinding<U> map(Function<T, U> map) {
		this.map = (Function) map;
		return (ModelBinding<U>) this;
	}

	/**
	 * The name of the property to bind to, or null for any property change
	 */
	public <P> ModelBinding<P> on(PropertyEnum fromPropertyName) {
		this.on = fromPropertyName;
		return (ModelBinding<P>) this;
	}

	/**
	 * The name of the property to bind to, or null for any property change
	 */
	public <P> ModelBinding<P> on(String fromPropertyName) {
		this.on = fromPropertyName;
		return (ModelBinding<P>) this;
	}

	/**
	 * Define the type of the incoming property
	 */
	public <V> ModelBinding<V> typed(Class<V> propertyType) {
		return (ModelBinding<V>) this;
	}

	/**
	 * Trigger the pipeline with the source's initial value when the binding is
	 * created (defaults to true)
	 */
	public ModelBinding<T> withSetOnInitialise(boolean setOnInitialise) {
		this.setOnInitialise = setOnInitialise;
		return this;
	}

	void acceptStreamElement(Object obj) {
		Consumer<Runnable> dispatch = ensureDispatch();
		if (dispatch == null) {
			acceptStreamElement0(obj);
		} else {
			dispatch.accept(() -> acceptStreamElement0(obj));
		}
	}

	Consumer<Runnable> ensureDispatch() {
		if (dispatchRef == null) {
			if (bindings.model().provideIsBound()) {
				dispatchRef = bindings.model().provideNode().getResolver()
						.dispatch();
			} else {
				// pre-binding, return null
				return null;
			}
		}
		return dispatchRef.get();
	}

	void acceptStreamElement0(Object obj) {
		Object o1 = supplier == null ? obj : supplier.get();
		if (preMapPredicate != null
				&& !((Predicate) preMapPredicate).test(o1)) {
			return;
		}
		Object o2 = map == null || (o1 == null && !transformsNull) ? o1
				: ((Function) map).apply(o1);
		if (predicate != null && !((Predicate) predicate).test(o2)) {
			return;
		}
		((Consumer) consumer).accept(o2);
	}

	void bind() {
		if (fromPropertyChangeSource != null) {
			listener = bindings.addPropertyChangeListener(
					fromPropertyChangeSource, on,
					evt -> acceptStreamElement(on != null ? evt.getNewValue()
							: fromPropertyChangeSource));
		} else if (fromTopic != null) {
			listener = fromTopic.add(t -> ((Consumer) consumer).accept(t))
					.asBinding();
			listener.bind();
		} else {
			throw new UnsupportedOperationException();
		}
	}

	void prepare() {
		if (setOnInitialise) {
			if (fromPropertyChangeSource != null) {
				acceptStreamElement(resolvePropertyChangeValue());
			} else if (fromTopic != null) {
				fromTopic.fireIfPublished((Consumer) consumer);
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	private Object resolvePropertyChangeValue() {
		return on != null ? Reflections.at(fromPropertyChangeSource)
				.property(on).get(fromPropertyChangeSource)
				: fromPropertyChangeSource;
	}

	public ModelBinding<T> filter(Predicate<T> predicate) {
		this.predicate = predicate;
		return this;
	}

	public ModelBinding<T> preMapFilter(Predicate<T> preMapPredicate) {
		this.preMapPredicate = preMapPredicate;
		return this;
	}

	public ModelBinding<?> withDeferredDispatch() {
		Preconditions.checkState(dispatchRef == null);
		dispatchRef = Ref
				.of(r -> Scheduler.get().scheduleDeferred(() -> r.run()));
		return this;
	}

	public TargetBinding to(SourcesPropertyChangeEvents to) {
		return new TargetBinding(to);
	}

	public class TargetBinding {
		SourcesPropertyChangeEvents to;

		Object on;

		TargetBinding(SourcesPropertyChangeEvents to) {
			this.to = to;
		}

		public TargetBinding on(String on) {
			this.on = on;
			return this;
		}

		public TargetBinding on(PropertyEnum on) {
			this.on = on;
			return this;
		}

		public void oneWay() {
			acceptLeftToRight();
		}

		private void acceptLeftToRight() {
			accept(newValue -> Reflections.at(to).property(on).set(to,
					newValue));
		}

		public <T2> ModelBinding<T2> bidi() {
			acceptLeftToRight();
			ModelBinding source = ModelBinding.this;
			ModelBinding reverse = new ModelBinding<>(bindings);
			reverse.fromPropertyChangeSource = to;
			reverse.on = on;
			TargetBinding reverseTargetBinding = reverse
					.to(source.fromPropertyChangeSource);
			reverseTargetBinding.on = source.on;
			reverseTargetBinding.acceptLeftToRight();
			return reverse;
		}
	}

	void unbind() {
		listener.unbind();
	}

	public <TE> ModelBinding<TE> from(Topic<TE> topic) {
		this.fromTopic = topic;
		return (ModelBinding<TE>) this;
	}
}