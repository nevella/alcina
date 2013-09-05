package cc.alcina.framework.common.client.collections;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;

import com.totsp.gwittir.client.beans.Converter;

public class PropertyMapper {
	private String[] fieldNameMappings;

	private boolean reverse;

	private PropertyAccessor propertyAccessor;

	private Map<String, Converter> leftConverters;

	private Map<String, Converter> rightConverters;

	public PropertyMapper(String[] fieldNameMappings, boolean reverse) {
		this.reverse = reverse;
		if (reverse) {
			String[] revMappings = new String[fieldNameMappings.length];
			for (int i = 0; i < fieldNameMappings.length; i += 2) {
				String key1 = fieldNameMappings[i];
				String key2 = fieldNameMappings[i + 1];
				revMappings[i] = key2;
				revMappings[i + 1] = key1;
			}
			fieldNameMappings = revMappings;
		}
		this.fieldNameMappings = fieldNameMappings;
		propertyAccessor = CommonLocator.get().propertyAccessor();
	}

	public void map(Object o1, Object o2) {
		map(o1, o2, null);
	}

	public void map(Object o1, Object o2, String propertyName) {
		if (reverse) {
			Object tmp = o2;
			o2 = o1;
			o1 = tmp;
		}
		try {
			for (int i = 0; i < fieldNameMappings.length; i += 2) {
				String key1 = fieldNameMappings[i];
				if (propertyName != null && !propertyName.equals(key1)) {
					continue;
				}
				String key2 = fieldNameMappings[i + 1];
				Object value = propertyName != null ? o1 : propertyAccessor
						.getPropertyValue(o1, key1);
				if (value != null) {
					Map<String, Converter> converters = reverse ? rightConverters
							: leftConverters;
					if (converters != null) {
						Converter converter = converters.get(key1);
						if (converter != null) {
							value = converter.convert(value);
						}
					}
					propertyAccessor.setPropertyValue(o2, key2, value);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void addLeftConverter(String leftPropertyName, Converter c) {
		if (leftConverters == null) {
			leftConverters = new LinkedHashMap<String, Converter>();
		}
		leftConverters.put(leftPropertyName, c);
	}

	public void addRightConverter(String rightPropertyName, Converter c) {
		if (rightConverters == null) {
			rightConverters = new LinkedHashMap<String, Converter>();
		}
		rightConverters.put(rightPropertyName, c);
	}

	public PropertyAccessor getPropertyAccessor() {
		return this.propertyAccessor;
	}

	public void setPropertyAccessor(PropertyAccessor propertyAccessor) {
		this.propertyAccessor = propertyAccessor;
	}
}
