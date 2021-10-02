package cc.alcina.framework.common.client.collections;

import java.util.function.Function;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;

//FIXME - mvcc.cascade - generally use an enum for propertynames
public class PropertyValueMapper<T, R> implements Function<T, R> {
	private PropertyReflector propertyReflector;

	public PropertyValueMapper(Class<T> clazz, String propertyName) {
		this.propertyReflector = Reflections.classLookup()
				.getPropertyReflector(clazz, propertyName);
	}

	@Override
	public R apply(T t) {
		return (R) propertyReflector.getPropertyValue(t);
	}
}
