package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor.IndividualPropertyAccessor;

public class PropertyPathAccesor {
	private String propertyPath;

	private String[] paths;

	private IndividualPropertyAccessor[] accessors = new IndividualPropertyAccessor[0];

	public PropertyPathAccesor(String propertyPath) {
		this.propertyPath = propertyPath;
		paths = propertyPath.split("\\.");
	}

	public Object getChainedProperty(Object obj) {
		if (obj == null) {
			return null;
		}
		if (paths.length == 1) {
			ensureAccessors(obj, 0);
			return accessors[0].getPropertyValue(obj);
		}
		int idx = 0;
		for (String path : paths) {
			if (obj == null) {
				break;
			}
			if (obj instanceof Collection) {
				List values = new ArrayList();
				for (Object member : (Collection) obj) {
					ensureAccessors(member, idx);
					Object value = accessors[idx].getPropertyValue(member);
					if (value instanceof Collection) {
						values.addAll((Collection) value);
					} else {
						values.add(value);
					}
				}
				obj = values;
			} else {
				ensureAccessors(obj, idx);
				obj = accessors[idx].getPropertyValue(obj);
			}
			idx++;
		}
		return obj;
	}

	private void ensureAccessors(Object obj, int idx) {
		if (accessors.length > idx || obj == null) {
			return;
		}
		String path = paths[idx];
		IndividualPropertyAccessor[] accessors = new IndividualPropertyAccessor[idx + 1];
		System.arraycopy(this.accessors, 0, accessors, 0, this.accessors.length);
		this.accessors = accessors;
		this.accessors[idx] = CommonLocator.get().propertyAccessor()
				.cachedAccessor(obj.getClass(), path);
	}

	@Override
	public String toString() {
		return "PropertyPathAccesor: " + propertyPath;
	}
}
