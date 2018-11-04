package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;

public class ShallowObjectFilter implements GraphProjectionFieldFilter {
	private Set<Class> allowOwningTypes;

	private boolean permitCollectionsAndMaps;

	private boolean logFilteredClasses;

	Set<Class> filteredClasses = new LinkedHashSet<>();

	public ShallowObjectFilter() {
		allowOwningTypes = new LinkedHashSet<Class>();
	}

	public ShallowObjectFilter(Collection<Class> allowOwningTypes) {
		this.allowOwningTypes = new LinkedHashSet<Class>(allowOwningTypes);
	}

	public boolean isLogFilteredClasses() {
		return this.logFilteredClasses;
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
		boolean primitiveOrDataClass = GraphProjection
				.isPrimitiveOrDataClass(type);
		if (primitiveOrDataClass) {
			return true;
		} else {
			if (logFilteredClasses && filteredClasses.add(forClass)) {
				Ax.out("Filtered: %s", forClass.getSimpleName());
			}
			return false;
		}
	}

	@Override
	public boolean permitTransient(Field field) {
		return false;
	}

	public void setLogFilteredClasses(boolean logFilteredClasses) {
		this.logFilteredClasses = logFilteredClasses;
	}

	public void setPermitCollectionsAndMaps(boolean permitCollectionsAndMaps) {
		this.permitCollectionsAndMaps = permitCollectionsAndMaps;
	}
}
