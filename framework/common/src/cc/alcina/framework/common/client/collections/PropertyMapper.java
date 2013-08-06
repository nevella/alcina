package cc.alcina.framework.common.client.collections;

import java.beans.PropertyDescriptor;
import java.util.Date;

import org.json.JSONObject;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.entity.SEUtilities;

import com.totsp.gwittir.client.beans.Converter;

public class PropertyMapper {
	private String[] fieldNameMappings;

	private boolean reverse;

	private PropertyAccessor propertyAccessor;
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
		propertyAccessor = CommonLocator.get()
				.propertyAccessor();
	}

	public void map(Object o1, Object o2) {
		if (reverse) {
			Object tmp = o2;
			o2 = o1;
			o1 = tmp;
		}
		
		try {
			for (int i = 0; i < fieldNameMappings.length; i += 2) {
				String key1 = fieldNameMappings[i];
				String key2 = fieldNameMappings[i + 1];
				Object value = propertyAccessor.getPropertyValue(o1, key1);
				if (value != null) {
					propertyAccessor.setPropertyValue(o2, key2, value);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public PropertyAccessor getPropertyAccessor() {
		return this.propertyAccessor;
	}

	public void setPropertyAccessor(PropertyAccessor propertyAccessor) {
		this.propertyAccessor = propertyAccessor;
	}
}
