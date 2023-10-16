package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.gwt.client.dirndl.model.Model.Bindings;

/**
 *
 * Build a binding pipeline associated with the parent model
 *
 * @param <T>
 */
public class ModelBindingBuilder<T> {
	Bindings bindings;

	SourcesPropertyChangeEvents from;

	PropertyEnum fromPropertyName;

	boolean oneWay;

	Function<?, ?> map;

	Supplier<?> supplier;

	Consumer<?> consumer;

	RemovablePropertyChangeListener listener;

	boolean setOnInitialise;

	public ModelBindingBuilder(Bindings bindings) {
		this.bindings = bindings;
	}

	/**
	 * add a terminal consumer (i.e. the actual action performer) to the end of
	 * the pipeline
	 */
	public void accept(Consumer<T> consumer) {
		this.consumer = consumer;
		bind();
	}

	/**
	 * When the change occurs, rather than pipe the event/change, pipe the
	 * object from <code>supplier</code>
	 */
	public <V> ModelBindingBuilder<V> consume(Supplier<V> supplier) {
		this.supplier = supplier;
		return (ModelBindingBuilder<V>) this;
	}

	/**
	 * The source of the binding property changes
	 */
	public ModelBindingBuilder<T> from(SourcesPropertyChangeEvents from) {
		this.from = from;
		return this;
	}

	/**
	 * Add an intermediate mapping to the pipeline
	 */
	public <U> ModelBindingBuilder<U> map(Function<T, U> map) {
		this.map = (Function) map;
		return (ModelBindingBuilder<U>) this;
	}

	/**
	 * The name of the property to bind to, or null for any property change
	 */
	public ModelBindingBuilder<T> on(PropertyEnum fromPropertyName) {
		this.fromPropertyName = fromPropertyName;
		return this;
	}

	/**
	 * Convert a bi-di bindings
	 */
	public ModelBindingBuilder<T> oneWay(boolean oneWay) {
		this.oneWay = oneWay;
		return this;
	}

	/**
	 * Define the type of the incoming property
	 */
	public <V> ModelBindingBuilder<V> typed(Class<V> propertyType) {
		return (ModelBindingBuilder<V>) this;
	}

	/**
	 * Trigger the pipeline with the source's initial value when the binding is
	 * created
	 */
	public ModelBindingBuilder<T> withSet() {
		this.setOnInitialise = true;
		return this;
	}

	void acceptStreamElement(Object obj) {
		Object o1 = supplier.get();
		Object o2 = ((Function) map).apply(o1);
		((Consumer) consumer).accept(o2);
	}

	void bind() {
		listener = bindings.addPropertyChangeListener(from, fromPropertyName,
				() -> acceptStreamElement(null));
		if (setOnInitialise) {
			acceptStreamElement(listener.currentValue());
		}
	}
}