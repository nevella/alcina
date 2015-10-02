package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MapObjectLookup;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;
import cc.alcina.framework.entity.projection.GraphProjection.InstantiateImplCallback;

public class GraphProjections {
	public static GraphProjections allow(Class... classes) {
		GraphProjections instance = new GraphProjections();
		instance.permittedClasses.addAll(Arrays.asList(classes));
		return instance;
	}

	public static GraphProjections defaultProjections() {
		GraphProjections instance = new GraphProjections();
		instance.fieldFilter = Registry.impl(PermissibleFieldFilter.class);
		return instance;
	}

	Set<Class> permittedClasses = new LinkedHashSet<Class>();

	Set<Class> forbiddenClasses = new LinkedHashSet<Class>();

	GraphProjection projector = new GraphProjection();

	private int maxDepth = Integer.MAX_VALUE;

	public GraphProjections maxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
		return this;
	}

	private GraphProjectionDataFilter dataFilter = Registry
			.impl(CollectionProjectionFilter.class);

	GraphProjectionFieldFilter fieldFilter = new PermissibleFieldFilterH();

	public GraphProjections dataFilter(GraphProjectionDataFilter dataFilter) {
		this.dataFilter = dataFilter;
		return this;
	}

	public GraphProjections fieldFilter(GraphProjectionFieldFilter fieldFilter) {
		this.fieldFilter = fieldFilter;
		return this;
	}

	public GraphProjections forbid(Class... classes) {
		forbiddenClasses.addAll(Arrays.asList(classes));
		return this;
	}

	public GraphProjections executor(GraphProjection projector) {
		this.projector = projector;
		return this;
	}

	public GraphProjections implCallback(InstantiateImplCallback callback) {
		dataFilter = Registry.impl(JPAImplementation.class).getResolvingFilter(
				callback, new DetachedEntityCache(), false);
		return this;
	}

	public <T> T project(T t) {
		try {
			projector.setFilters(fieldFilter, dataFilter);
			projector.setMaxDepth(maxDepth);
			return projector.project(t, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public GraphProjections strict() {
		fieldFilter = new StrictAllowForbid();
		return this;
	}

	public class StrictAllowForbid implements GraphProjectionFieldFilter {
		@Override
		public Boolean permitClass(Class clazz) {
			if (!HasIdAndLocalId.class.isAssignableFrom(clazz)) {
				return true;
			}
			if (permittedClasses.size() > 0
					&& !permittedClasses.contains(clazz)) {
				return false;
			}
			if (forbiddenClasses.size() > 0 && forbiddenClasses.contains(clazz)) {
				return false;
			}
			return true;
		}

		@Override
		public boolean permitField(Field field,
				Set<Field> perObjectPermissionFields, Class clazz) {
			return true;
		}

		@Override
		public boolean permitTransient(Field field) {
			return false;
		}
	}

	class PermissibleFieldFilterH extends PermissibleFieldFilter {
		@Override
		public Boolean permitClass(Class clazz) {
			if (!HasIdAndLocalId.class.isAssignableFrom(clazz)) {
				return true;
			}
			if (permittedClasses.size() > 0
					&& !permittedClasses.contains(clazz)) {
				return false;
			}
			if (forbiddenClasses.size() > 0 && forbiddenClasses.contains(clazz)) {
				return false;
			}
			return super.permitClass(clazz);
		}
	}

	public static MapObjectLookup reachableForClasses(Object target,
			Class... classes) {
		CollectionProjectionFilterWithCache dataFilter = (CollectionProjectionFilterWithCache) Registry
				.impl(CollectionProjectionFilter.class);
		allow(classes).dataFilter(dataFilter).project(target);
		return dataFilter.getObjectLookup();
	}

	public static MapObjectLookup reachable(Object target) {
		CollectionProjectionFilterWithCache dataFilter = (CollectionProjectionFilterWithCache) Registry
				.impl(CollectionProjectionFilter.class);
		defaultProjections().dataFilter(dataFilter).project(target);
		return dataFilter.getObjectLookup();
	}

	public static class CountingProjector extends GraphProjection {
		public CountingMap<Class> counts = new CountingMap<Class>();

		@Override
		protected <T> T newInstance(Class sourceClass) throws Exception {
			counts.add(sourceClass);
			return super.newInstance(sourceClass);
		}
	}
}
