package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;

public class ShallowObjectFilter implements GraphProjectionFieldFilter {
	private Set<Class> allowOwningTypes;

	private boolean permitCollectionsAndMaps;

	public ShallowObjectFilter() {
		allowOwningTypes = new LinkedHashSet<Class>();
	}

	public ShallowObjectFilter(Collection<Class> allowOwningTypes) {
		this.allowOwningTypes = new LinkedHashSet<Class>(allowOwningTypes);
	}

	public boolean isPermitCollectionsAndMaps() {
		return this.permitCollectionsAndMaps;
	}

	@Override
	public Boolean permitClass(Class clazz) {
		return true;
	}

	public ShallowObjectFilter permitCollectionsAndMaps() {
		this.permitCollectionsAndMaps = true;
		return this;
	}

	@Override
	public boolean permitField(Field field,
			Set<Field> perObjectPermissionFields, Class forClass) {
		if (allowOwningTypes.contains(forClass)) {
			return true;
		}
		if (permitCollectionsAndMaps) {
			if (Collection.class.isAssignableFrom(field.getType())
					|| Map.class.isAssignableFrom(field.getType())) {
				return true;
			}
		}
		Class<?> type = field.getType();
		return GraphProjection.isPrimitiveOrDataClass(type);
	}

	@Override
	public boolean permitTransient(Field field) {
		return false;
	}

	public void setPermitCollectionsAndMaps(boolean permitCollectionsAndMaps) {
		this.permitCollectionsAndMaps = permitCollectionsAndMaps;
	}
}
