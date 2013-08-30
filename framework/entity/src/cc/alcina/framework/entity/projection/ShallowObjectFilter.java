package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;

public class ShallowObjectFilter implements GraphProjectionFieldFilter {
	private Set<Class> allowOwningTypes;

	public ShallowObjectFilter() {
		allowOwningTypes = new LinkedHashSet<Class>();
	}

	public ShallowObjectFilter(Set<Class> allowOwningTypes) {
		this.allowOwningTypes = allowOwningTypes;
	}


	@Override
	public Boolean permitClass(Class clazz) {
		return true;
	}

	@Override
	public boolean permitField(Field field,
			Set<Field> perObjectPermissionFields, Class forClass) {
		if (allowOwningTypes.contains(forClass)) {
			return true;
		}
		Class<?> type = field.getType();
		return GraphProjection.isPrimitiveOrDataClass(type);
	}

	@Override
	public boolean permitTransient(Field field) {
		return false;
	}
}
