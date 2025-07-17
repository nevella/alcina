package cc.alcina.framework.common.client.logic.reflection;

import java.util.function.Supplier;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

public class InstanceProperty<S extends SourcesPropertyChangeEvents, T> {
	public S source;

	public TypedProperty<S, T> property;

	public InstanceProperty(S source, TypedProperty<S, T> property) {
		this.source = source;
		this.property = property;
	}

	public interface SourceSupplier<S> {
		S getSource();
	}

	public T get() {
		return property.get(getSource());
	}

	public void set(T t) {
		property.set(getSource(), t);
	}

	public S getSource() {
		return source;
	}

	/*
	 * A base for generated classes containing instance properties
	 */
	public static abstract class Container<S> {
		protected S source;

		public Container(S source) {
			this.source = source;
		}
	}
}
