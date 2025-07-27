package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.ListenerBinding;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model.Bindings;

/**
 *
 * <p>
 * Build a binding pipeline with a lifecycle controlled by the registering
 * model. The logic and api are similar to the {@link java.util.stream.Stream}
 * api.
 *
 * <p>
 * FIXME - dirndl - this could be refactored to an "event source" and an "event
 * stream" - acceptStreamElement0 being essentially the stream part. But that
 * would require yet more footprint for what was originally lightweight - so
 * holding pattern
 * 
 * <p>
 * Note - when bidi binding a data model to a UI element, the data model element
 * should be 'from' (since the initial bind sequence is from-&gt;to then
 * to-&gt;from)
 * 
 * <p>
 * The {@link IfNotEqual} interface can be used to prevent bindings from
 * actually firing if the existing property value is 'good enough'
 * 
 * <p>
 * One of the nicest bits of this is {@link #dispatchRef} - avoiding *a lot* of
 * the threading cruft of traditional desktop apps by mandating that a
 * multi-threaded (romcom) app propagate changes - and thus UI mutations - on
 * the app's UI thread. There's a similar system for {@link InstanceOracle}
 * 
 * @param <T>
 */
public class ModelBinding<T> {
	public static class TargetBinding<BSP extends SourcesPropertyChangeEvents, T2> {
		BSP to;

		Object on;

		Function map;

		ModelBinding<T2> binding;

		Property property;

		TargetBinding(ModelBinding<T2> binding, BSP to) {
			this.binding = binding;
			binding.targetBinding = this;
			this.to = to;
		}

		public ModelBinding<T2> bidi() {
			acceptLeftToRight();
			ModelBinding source = binding;
			ModelBinding<?> reverse = new ModelBinding<>(binding.bindings);
			binding.bindings.modelBindings.add(reverse);
			reverse.fromPropertyChangeSource = to;
			reverse.map = map;
			reverse.on = on;
			reverse.transformsNull = binding.transformsNull;
			TargetBinding reverseTargetBinding = reverse
					.to(source.fromPropertyChangeSource);
			reverseTargetBinding.on = source.on;
			reverseTargetBinding.acceptLeftToRight();
			return (ModelBinding<T2>) reverse;
		}

		public TargetBinding<BSP, ?> onUntyped(PropertyEnum on) {
			this.on = on;
			return this;
		}

		public TargetBinding<BSP, T2>
				on(TypedProperty<? super BSP, ? super T2> on) {
			this.on = on;
			return (TargetBinding<BSP, T2>) this;
		}

		public <T3> TargetBinding map(Function<T2, T3> map) {
			this.map = (Function) map;
			return this;
		}

		public TargetBinding on(String on) {
			this.on = on;
			return this;
		}

		public TargetBinding withFireOnce() {
			binding.fireOnce = true;
			return this;
		}

		public void oneWay() {
			acceptLeftToRight();
		}

		@Override
		public String toString() {
			return ensureProperty().toLocationString();
		}

		Object getExistingValue() {
			return ensureProperty().get(to);
		}

		Class<?> getTargetPropertyType() {
			return ensureProperty().getType();
		}

		void acceptLeftToRight() {
			binding.accept(newValue -> {
				try {
					ensureProperty().set(to, newValue);
				} catch (Exception e) {
					LoggerFactory.getLogger(getClass()).warn(
							"Exception executing model binding on {} to {} :: {}",
							NestedName.get(binding.bindings.model()),
							NestedName.get(to), on);
					e.printStackTrace();
					throw WrappedRuntimeException.wrap(e);
				}
			});
		}

		private Property ensureProperty() {
			if (property == null) {
				Preconditions.checkNotNull(on);
				property = Reflections.at(to).property(on);
			}
			return property;
		}
	}

	Bindings bindings;

	SourcesPropertyChangeEvents fromPropertyChangeSource;

	Object on;

	Function<?, ?> map;

	Supplier<?> supplier;

	Consumer<?> consumer;

	ListenerBinding listener;

	boolean setOnInitialise = true;

	Ref<Consumer<Runnable>> dispatchRef = null;

	Predicate<T> postMapPredicate;

	boolean transformsNull;

	Predicate<T> preSupplierPredicate;

	Predicate<T> preMapPredicate;

	Topic<?> fromTopic;

	/*
	 * Use this to add conditional breakpoints to test things like unwanted
	 * multiple dispatch
	 */
	boolean debug;

	/*
	 * only fire once
	 */
	boolean fireOnce;

	boolean fired;

	TargetBinding targetBinding;

	boolean ifNotEqual;

	public Class<? extends NodeEvent> fromNodeEventClass;

	public ModelBinding(Bindings bindings) {
		this.bindings = bindings;
	}

	public ModelBinding ifNotEqual() {
		ifNotEqual = true;
		return this;
	}

	/**
	 * add a terminal consumer (i.e. the actual action performer) to the end of
	 * the pipeline
	 */
	public void accept(Consumer<T> consumer) {
		this.consumer = consumer;
	}

	/**
	 * Depending on whether map() or value()/supplier() have already been
	 * called, this filter will be inserted into a different location in the
	 * binding pipeline (i.e. after whatever the most recent stream operation
	 * is)
	 * 
	 * @param predicate
	 * @return
	 */
	public ModelBinding<T> filter(Predicate<T> predicate) {
		Predicate existingFilter = null;
		if (map != null) {
			existingFilter = this.postMapPredicate;
			this.postMapPredicate = predicate;
		} else if (supplier != null) {
			existingFilter = this.preMapPredicate;
			this.preMapPredicate = predicate;
		} else {
			existingFilter = preSupplierPredicate;
			this.preSupplierPredicate = predicate;
		}
		Preconditions.checkState(existingFilter == null,
				"Cannot set multiple predicates");
		return this;
	}

	public ModelBinding<T> nonNull() {
		return filter(Objects::nonNull);
	}

	public ModelBinding<T> debug() {
		this.debug = true;
		return this;
	}

	/**
	 * The source of the binding property changes
	 */
	ModelBinding<T> from(SourcesPropertyChangeEvents from) {
		Preconditions.checkNotNull(from);
		this.fromPropertyChangeSource = from;
		return this;
	}

	<TE> ModelBinding<TE> from(Topic<TE> topic) {
		this.fromTopic = topic;
		return (ModelBinding<TE>) this;
	}

	<E extends NodeEvent> ModelBinding<E>
			fromNodeEventClass(Class<E> nodeEventClass) {
		this.fromNodeEventClass = nodeEventClass;
		return (ModelBinding<E>) this;
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
	public <PT> ModelBinding<PT> on(TypedProperty<?, PT> typedProperty) {
		this.on = typedProperty;
		return (ModelBinding<PT>) this;
	}

	/**
	 * The name of the property to bind to, or null for any property change
	 */
	public <P> ModelBinding<P> on(String fromPropertyName) {
		this.on = fromPropertyName;
		return (ModelBinding<P>) this;
	}

	/**
	 * add a terminal runnable (i.e. the actual action performer) to the end of
	 * the pipeline
	 */
	public void signal(Runnable runnable) {
		this.consumer = t -> runnable.run();
	}

	public <BSP extends SourcesPropertyChangeEvents> TargetBinding<BSP, T>
			to(BSP to) {
		Preconditions.checkNotNull(to);
		return new TargetBinding(this, to);
	}

	public <BSP extends SourcesPropertyChangeEvents, T2> TargetBinding
			to(InstanceProperty<BSP, T2> instanceProperty) {
		return to(instanceProperty.source)
				.on((TypedProperty) instanceProperty.property);
	}

	/**
	 * Define the type of the incoming property
	 */
	public <V> ModelBinding<V> typed(Class<V> propertyType) {
		return (ModelBinding<V>) this;
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

	public ModelBinding<?> withDeferredDispatch() {
		Preconditions.checkState(dispatchRef == null);
		dispatchRef = Ref
				.of(r -> Scheduler.get().scheduleDeferred(() -> r.run()));
		return this;
	}

	public ModelBinding<?> withFinalDispatch() {
		Preconditions.checkState(dispatchRef == null);
		dispatchRef = Ref
				.of(r -> Scheduler.get().scheduleFinally(() -> r.run()));
		return this;
	}

	/**
	 * Trigger the pipeline with the source's initial value when the binding is
	 * created (defaults to true)
	 */
	public ModelBinding<T> withSetOnInitialise(boolean setOnInitialise) {
		this.setOnInitialise = setOnInitialise;
		return this;
	}

	public ModelBinding<T> withTransformsNull() {
		this.transformsNull = true;
		return this;
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder();
		format.format("%s.", NestedName.get(fromPropertyChangeSource));
		format.format("%s", on == null ? "*" : fromProperty().getName());
		// ...map, fn etc
		if (targetBinding != null) {
			format.format(" --> %s", targetBinding);
		}
		return format.toString();
	}

	void acceptStreamElement(Object obj) {
		if (fireOnce) {
			if (fired) {
				return;
			}
		}
		fired = true;
		Consumer<Runnable> dispatch = ensureDispatch();
		if (dispatch == null) {
			acceptStreamElement0(obj);
		} else {
			dispatch.accept(() -> acceptStreamElement0(obj));
		}
	}

	void acceptStreamElement0(Object obj) {
		if (preSupplierPredicate != null
				&& !((Predicate) preSupplierPredicate).test(obj)) {
			return;
		}
		Object o1 = supplier == null ? obj : supplier.get();
		if (preMapPredicate != null
				&& !((Predicate) preMapPredicate).test(o1)) {
			return;
		}
		Object o2 = map == null || (o1 == null && !transformsNull) ? o1
				: ((Function) map).apply(o1);
		if (postMapPredicate != null
				&& !((Predicate) postMapPredicate).test(o2)) {
			return;
		}
		if (targetBinding != null) {
			Class<?> toType = targetBinding.getTargetPropertyType();
			if (Reflections.isAssignableFrom(IfNotEqual.class, toType)
					|| ifNotEqual) {
				Object existing = targetBinding.getExistingValue();
				if (existing != null) {
					if (Objects.equals(existing, o2)) {
						return;
					}
				}
			}
		}
		Preconditions.checkState(consumer != null,
				"No consumer - possibly you forgot to call bind(), "
						+ "or  onewWay()/bidi() if this is a property binding");
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
			// noop (from is null)
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

	void prepare() {
		try {
			if (setOnInitialise) {
				if (fromPropertyChangeSource != null) {
					acceptStreamElement(resolvePropertyChangeValue());
				} else if (fromTopic != null) {
					fromTopic.fireIfPublished((Consumer) consumer);
				} else {
					// noop - from is null
				}
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass()).warn(
					"Exception preparing model binding on {} from {} :: {}",
					NestedName.get(bindings.model()),
					NestedName.get(fromPropertyChangeSource), on);
			throw WrappedRuntimeException.wrap(e);
		}
	}

	void unbind() {
		if (listener != null) {
			listener.unbind();
		}
	}

	private Object resolvePropertyChangeValue() {
		return on != null ? fromProperty().get(fromPropertyChangeSource)
				: fromPropertyChangeSource;
	}

	private Property fromProperty() {
		return Reflections.at(fromPropertyChangeSource).property(on);
	}

	public <E> void emit(Class<? extends ModelEvent<E, ?>> modelEventClass) {
		emit(modelEventClass, null);
	}

	/* A terminal, stream output will emit the corresponding event */
	public <E> void emit(Class<? extends ModelEvent<E, ?>> modelEventClass,
			E value) {
		signal(() -> bindings.model().emitEvent(modelEventClass, value));
	}
}