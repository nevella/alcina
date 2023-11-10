package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.gwt.client.dirndl.model.Model.Bindings;

/**
 *
 * Build a binding pipeline associated with the parent model
 *
 * @param <T>
 */
public class ModelBinding<T> {
	Bindings bindings;

	SourcesPropertyChangeEvents from;

	Object fromPropertyName;

	boolean oneWay;

	Function<?, ?> map;

	Supplier<?> supplier;

	Consumer<?> consumer;

	RemovablePropertyChangeListener listener;

	boolean setOnInitialise = true;

	Ref<Consumer<Runnable>> dispatchRef = null;

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
		this.from = from;
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
		this.fromPropertyName = fromPropertyName;
		return (ModelBinding<P>) this;
	}

	/**
	 * The name of the property to bind to, or null for any property change
	 */
	public <P> ModelBinding<P> on(String fromPropertyName) {
		this.fromPropertyName = fromPropertyName;
		return (ModelBinding<P>) this;
	}

	/**
	 * Convert a bi-di bindings
	 */
	public ModelBinding<T> oneWay(boolean oneWay) {
		this.oneWay = oneWay;
		return this;
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
		Object o2 = map == null ? o1 : ((Function) map).apply(o1);
		((Consumer) consumer).accept(o2);
	}

	void bind() {
	}

	void prepare() {
		listener = bindings.addPropertyChangeListener(from, fromPropertyName,
				evt -> acceptStreamElement(evt.getNewValue()));
		if (setOnInitialise) {
			acceptStreamElement(listener.currentValue());
		}
	}
}