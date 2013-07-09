package cc.alcina.framework.common.client.collections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.totsp.gwittir.client.beans.Converter;

public class PropertyConverter<I,O> implements Converter<I,O> {


	private String key;




	public PropertyConverter(String key) {
		this.key = key;
	}


	

	@Override
	public O convert(I o) {
		Object propertyValue = CommonLocator.get().propertyAccessor()
				.getPropertyValue(o, key);
		return (O) propertyValue;
	}
}
