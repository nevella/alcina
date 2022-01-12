package cc.alcina.framework.common.client.collections;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.util.PropertyPath;

public class PropertyConverter<I, O> implements Converter<I, O> {
	private PropertyPath accessor;

	public PropertyConverter(String path) {
		this.accessor = new PropertyPath(path);
	}

	@Override
	public O convert(I o) {
		Object propertyValue = accessor.getChainedProperty(o);
		return (O) propertyValue;
	}

	public static class LongIdPropertyConverter<I>
			extends PropertyConverter<I, Long> {
		public LongIdPropertyConverter() {
			super("id");
		}
	}
}
