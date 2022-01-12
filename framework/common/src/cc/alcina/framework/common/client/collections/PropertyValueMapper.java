package cc.alcina.framework.common.client.collections;

import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

//FIXME - mvcc.cascade - generally use an enum for propertynames
public class PropertyValueMapper<T, R> implements Function<T, R> {
	private Property property;

	public PropertyValueMapper(Class<T> clazz, String propertyName) {
		this.property = Reflections
				.getPropertyReflector(clazz, propertyName);
	}

	@Override
	public R apply(T t) {
		return (R) property.getPropertyValue(t);
	}
}
