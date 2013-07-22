package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;

public class PropertyPathAccesor {
	private String propertyPath;

	private String[] paths;

	public PropertyPathAccesor(String propertyPath) {
		this.propertyPath = propertyPath;
		paths = propertyPath.split("\\.");
	}

	public Object getChainedProperty(Object obj) {
		if (paths.length == 1) {
			return CommonLocator.get().propertyAccessor()
					.getPropertyValue(obj, paths[0]);
		}
		for (String path : paths) {
			if (obj == null) {
				break;
			}
			if (obj instanceof Collection) {
				List values = new ArrayList();
				for (Object member : (Collection) obj) {
					Object value = CommonLocator.get().propertyAccessor()
							.getPropertyValue(member, path);
					if (value instanceof Collection) {
						values.addAll((Collection) value);
					} else {
						values.add(value);
					}
				}
				obj=values;
			} else {
				obj = CommonLocator.get().propertyAccessor()
						.getPropertyValue(obj, path);
			}
		}
		return obj;
	}

	@Override
	public String toString() {
		return "PropertyPathAccesor: " + propertyPath;
	}
}
