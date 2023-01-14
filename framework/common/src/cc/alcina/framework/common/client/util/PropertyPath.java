package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

public class PropertyPath {
	private String propertyPath;

	private String[] paths;

	private Property[] accessors = new Property[0];

	public PropertyPath(String propertyPath) {
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

	public boolean hasPath(Object obj) {
		if (obj == null) {
			throw new NoSuchElementException();
		}
		try {
			getChainedProperty(obj);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	public boolean isSinglePathSegment() {
		return paths.length == 1;
	}

	public void setChainedProperty(Object obj, Object value) {
		if (isSinglePathSegment()) {
			ensureAccessors(obj, 0);
			accessors[0].set(obj, value);
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
				obj = accessors[idx].get(obj);
			}
			idx++;
		}
		assert idx == paths.length - 1;
		ensureAccessors(obj, idx);
		accessors[idx].set(obj, value);
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
		Property[] accessors = new Property[idx + 1];
		System.arraycopy(this.accessors, 0, accessors, 0,
				this.accessors.length);
		this.accessors = accessors;
		Property property = Reflections.at(obj).property(path);
		if (property == null) {
			throw new NoSuchElementException(
					Ax.format("No such element for path '%s' at index %s [%s]",
							propertyPath, idx, path));
		}
		this.accessors[idx] = property;
	}

	private Object get(Object obj, boolean type) {
		if (obj == null) {
			return null;
		}
		if (isSinglePathSegment()) {
			ensureAccessors(obj, 0);
			Property accessor = accessors[0];
			return type ? accessor.getType() : accessor.get(obj);
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
					Object value = accessors[idx].get(member);
					if (value instanceof Collection) {
						values.addAll((Collection) value);
					} else {
						values.add(value);
					}
				}
				obj = values;
			} else {
				ensureAccessors(obj, idx);
				obj = accessors[idx].get(obj);
			}
			idx++;
		}
		return obj;
	}
}
