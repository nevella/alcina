package cc.alcina.framework.common.client.collections;

import cc.alcina.framework.common.client.util.PropertyPathAccesor;

import com.totsp.gwittir.client.beans.Converter;

public class PropertyConverter<I, O> implements Converter<I, O> {
	private PropertyPathAccesor accessor;

	public PropertyConverter(String path) {
		this.accessor = new PropertyPathAccesor(path);
	}

	@Override
	public O convert(I o) {
		Object propertyValue = accessor.getChainedProperty(o);
		return (O) propertyValue;
	}
}
