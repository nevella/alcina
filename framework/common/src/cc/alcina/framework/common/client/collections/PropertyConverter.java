package cc.alcina.framework.common.client.collections;

import cc.alcina.framework.common.client.util.PropertyPathAccessor;

import com.totsp.gwittir.client.beans.Converter;

public class PropertyConverter<I, O> implements Converter<I, O> {
	private PropertyPathAccessor accessor;

	public PropertyConverter(String path) {
		this.accessor = new PropertyPathAccessor(path);
	}

	@Override
	public O convert(I o) {
		Object propertyValue = accessor.getChainedProperty(o);
		return (O) propertyValue;
	}
}
