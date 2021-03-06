package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;

public class PropertyPathAccessor {
	private String propertyPath;

	private String[] paths;

	private PropertyReflector[] accessors = new PropertyReflector[0];

	public PropertyPathAccessor(String propertyPath) {
		this.propertyPath = propertyPath;
		paths = propertyPath.split("\\.");
	}

	public Object getChainedProperty(Object obj) {
		return get(obj, false);
	}

	public Class getChainedPropertyType(Object obj) {
		return (Class) get(obj, true);
	}

	public String[] getPaths() {
		return this.paths;
	}

	public String getPropertyPath() {
		return this.propertyPath;
	}

	public void setChainedProperty(Object obj, Object value) {
		if (paths.length == 1) {
			ensureAccessors(obj, 0);
			accessors[0].setPropertyValue(obj, value);
		}
		int idx = 0;
		for (; idx < paths.length - 1;) {
			if (obj == null) {
				throw new RuntimeException("property path set hit a null");
			}
			if (obj instanceof Collection) {
				throw new RuntimeException(
						"set with property path does not support collection properties");
			} else {
				ensureAccessors(obj, idx);
				obj = accessors[idx].getPropertyValue(obj);
			}
			idx++;
		}
		assert idx == paths.length - 1;
		ensureAccessors(obj, idx);
		accessors[idx].setPropertyValue(obj, value);
	}

	@Override
	public String toString() {
		return "PropertyPathAccesor: " + propertyPath;
	}

	private void ensureAccessors(Object obj, int idx) {
		if (accessors.length > idx || obj == null) {
			return;
		}
		String path = paths[idx];
		PropertyReflector[] accessors = new PropertyReflector[idx + 1];
		System.arraycopy(this.accessors, 0, accessors, 0,
				this.accessors.length);
		this.accessors = accessors;
		this.accessors[idx] = Reflections.propertyAccessor()
				.getPropertyReflector(obj.getClass(), path);
	}

	private Object get(Object obj, boolean type) {
		if (obj == null) {
			return null;
		}
		if (paths.length == 1) {
			ensureAccessors(obj, 0);
			PropertyReflector accessor = accessors[0];
			return type ? accessor.getPropertyType()
					: accessor.getPropertyValue(obj);
		}
		if (type) {
			return null;
		}
		int idx = 0;
		for (String path : paths) {
			if (obj == null) {
				break;
			}
			if (obj instanceof Collection) {
				List values = new ArrayList();
				for (Object member : (Collection) obj) {
					if (member == null) {
						continue;
					}
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
}
