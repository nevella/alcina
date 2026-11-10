package cc.alcina.framework.common.client.collections;

import java.util.function.Function;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

public class PropertyValueMapper<T, R> implements Function<T, R> {
	private Property property;

	public PropertyValueMapper(Class<T> clazz, String propertyName) {
		this.property = Reflections.at(clazz).property(propertyName);
	}

	@Override
	public R apply(T t) {
		return (R) property.get(t);
	}
}
