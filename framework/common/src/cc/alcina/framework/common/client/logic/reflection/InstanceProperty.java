package cc.alcina.framework.common.client.logic.reflection;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

public class InstanceProperty<S extends SourcesPropertyChangeEvents, T> {
	public S source;

	public TypedProperty<S, T> property;

	public InstanceProperty(S source, TypedProperty<S, T> property) {
		this.source = source;
		this.property = property;
	}

	public T get() {
		return property.get(source);
	}

	public void set(T t) {
		property.set(source, t);
	}

	public S getSource() {
		return source;
	}
}
