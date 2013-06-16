package cc.alcina.framework.entity.util;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;


public class ShallowObjectFilter implements GraphProjectionFilter {
	private Set<Class> allowOwningTypes;

	public ShallowObjectFilter() {
		allowOwningTypes=new LinkedHashSet<Class>();
	}

	public ShallowObjectFilter(Set<Class> allowOwningTypes) {
		this.allowOwningTypes = allowOwningTypes;
	}

	@Override
	public <T> T filterData(T original, T projected,
			GraphProjectionContext context, GraphProjection graphProjection)
			throws Exception {
		return null;
	}

	@Override
	public boolean permitField(Field field, Set<Field> perObjectPermissionFields) {
		if(allowOwningTypes.contains(field.getDeclaringClass())){
			return true;
		}
		Class<?> type = field.getType();
		return GraphProjection.isPrimitiveOrDataClass(type);
	}
}
