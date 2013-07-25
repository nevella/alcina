package cc.alcina.framework.entity.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.entityaccess.DetachedEntityCache;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.util.GraphProjection.CollectionProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;
import cc.alcina.framework.entity.util.GraphProjection.PermissibleFieldFilter;

public class GraphProjections {
	Set<Class> permittedClasses = new LinkedHashSet<Class>();

	Set<Class> forbiddenClasses = new LinkedHashSet<Class>();

	private GraphProjectionFilter dataFilter = new CollectionProjectionFilter();

	public static GraphProjections allow(Class... classes) {
		GraphProjections instance = new GraphProjections();
		instance.permittedClasses.addAll(Arrays.asList(classes));
		return instance;
	}

	public GraphProjections forbid(Class... classes) {
		GraphProjections instance = new GraphProjections();
		instance.forbiddenClasses.addAll(Arrays.asList(classes));
		return instance;
	}

	public GraphProjections implCallback(InstantiateImplCallback callback) {
		dataFilter = EntityLayerLocator.get().jpaImplementation()
				.getResolvingFilter(callback, new DetachedEntityCache());
		return this;
	}

	public <T> T project(T t) {
		try {
			return new GraphProjection(new PermissibleFieldFilterH(),
					dataFilter).project(t, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public GraphProjectionFilter fieldFilter() {
		return new PermissibleFieldFilterH();
	}

	class PermissibleFieldFilterH extends PermissibleFieldFilter {
		@Override
		public boolean permitField(Field field,
				Set<Field> perObjectPermissionFields) {
			Class<?> type = field.getType();
			if (!GraphProjection.isPrimitiveOrDataClass(type)) {
				if (Collection.class.isAssignableFrom(type)) {
					Type pt = GraphProjection.getGenericType(field);
					if (pt instanceof ParameterizedType) {
						type = (Class) ((ParameterizedType) pt)
								.getActualTypeArguments()[0];
					}
				}
				if (permittedClasses.size() > 0
						&& !permittedClasses.contains(type)) {
					return false;
				}
				if (forbiddenClasses.size() > 0
						&& forbiddenClasses.contains(type)) {
					return false;
				}
			}
			return super.permitField(field, perObjectPermissionFields);
		}
	}
}
