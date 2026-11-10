package cc.alcina.framework.common.client.collections;

import java.util.function.Function;

import cc.alcina.framework.common.client.util.PropertyPath;

public class PropertyConverter<I, O> implements Function<I, O> {
	private PropertyPath accessor;

	public PropertyConverter(String path) {
		this.accessor = new PropertyPath(path);
	}

	@Override
	public O apply(I o) {
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
