package cc.alcina.framework.servlet.sync;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;

import com.totsp.gwittir.client.beans.Converter;

public class SyncLandscapeHelper {
	public void nonHiliPropertiesToObjectData(Object source,
			SyncObjectData data, SyncLandscape landscape) {
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(
					source.getClass()).getPropertyDescriptors();
			PropertyDescriptor result = null;
			for (PropertyDescriptor pd : pds) {
				SyncConversionSpec spec = landscape.getConversionSpec(pd
						.getName());
				if (spec == null) {
					continue;
				}
				Object value = CommonLocator.get().propertyAccessor()
						.getPropertyValue(source, pd.getName());
				Converter converter = spec.getConverter();
				if (value != null) {
					if (converter != null) {
						value = converter.convert(value);
					}
					data.getNonNullValues().put(pd.getName(), value);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
