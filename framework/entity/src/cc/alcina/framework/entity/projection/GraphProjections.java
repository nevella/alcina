package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MapObjectLookup;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Mvcc;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
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

	public static MapObjectLookup reachableForClasses(Object target,
			Class... classes) {
		CollectionProjectionFilterWithCache dataFilter = (CollectionProjectionFilterWithCache) Registry
				.impl(CollectionProjectionFilter.class);
		allow(classes).dataFilter(dataFilter).project(target);
		return dataFilter.getObjectLookup();
	}

	public static Multimap<Class, List> reachableForClasses(Object target,
			Predicate<Class> classFilter) {
		IdentityDataFilter dataFilter = new IdentityDataFilter();
		allow(classFilter).dataFilter(dataFilter).project(target);
		return dataFilter.byClass;
	}

	private static GraphProjections allow(Predicate<Class> classFilter) {
		GraphProjections instance = new GraphProjections();
		instance.classFilter = classFilter;
		return instance;
	}

	private Predicate<Class> classFilter;

	Set<Class> permittedClasses = new LinkedHashSet<Class>();

	Set<Class> forbiddenClasses = new LinkedHashSet<Class>();

	GraphProjection projector = new GraphProjection();

	private int maxDepth = Integer.MAX_VALUE;

	private GraphProjectionDataFilter dataFilter = Registry
			.impl(CollectionProjectionFilter.class);

	GraphProjectionFieldFilter fieldFilter = new PermissibleFieldFilterH();

	private boolean collectionReachedCheck = true;

	public GraphProjections dataFilter(GraphProjectionDataFilter dataFilter) {
		this.dataFilter = dataFilter;
		return this;
	}

	public GraphProjections executor(GraphProjection projector) {
		this.projector = projector;
		return this;
	}

	public GraphProjections
			fieldFilter(GraphProjectionFieldFilter fieldFilter) {
		this.fieldFilter = fieldFilter;
		return this;
	}

	public GraphProjections forbid(Class... classes) {
		forbiddenClasses.addAll(Arrays.asList(classes));
		return this;
	}

	public GraphProjections implCallback(InstantiateImplCallback callback) {
		dataFilter = Registry.impl(JPAImplementation.class)
				.getResolvingFilter(callback, new DetachedEntityCache(), false);
		return this;
	}

	public GraphProjections maxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
		return this;
	}

	public GraphProjections noCollectionReachedCheck() {
		collectionReachedCheck = false;
		return this;
	}

	public <T> T project(T t) {
		try {
			projector.setFilters(fieldFilter, dataFilter);
			projector.setMaxDepth(maxDepth);
			projector.setCollectionReachedCheck(collectionReachedCheck);
			return projector.project(t, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public GraphProjections strict() {
		fieldFilter = new StrictAllowForbid();
		return this;
	}

	public static class CountingProjector extends GraphProjection {
		public CountingMap<Class> counts = new CountingMap<Class>();

		@Override
		protected <T> T newInstance(Class sourceClass,
				GraphProjectionContext context) throws Exception {
			counts.add(sourceClass);
			return super.newInstance(sourceClass, context);
		}
	}

	public class StrictAllowForbid implements GraphProjectionFieldFilter {
		@Override
		public Boolean permitClass(Class clazz) {
			if (!Entity.class.isAssignableFrom(clazz)) {
				return true;
			}
			clazz = Mvcc.resolveEntityClass(clazz);
			if (permittedClasses.size() > 0
					&& !permittedClasses.contains(clazz)) {
				return false;
			}
			if (forbiddenClasses.size() > 0
					&& forbiddenClasses.contains(clazz)) {
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

	private static class IdentityDataFilter
			extends CollectionProjectionFilterWithCache {
		Multimap<Class, List> byClass = new Multimap<>();

		@Override
		public <T> T filterData(T original, T projected,
				GraphProjectionContext context, GraphProjection graphProjection)
				throws Exception {
			if (original instanceof Collection || original instanceof Map) {
				return super.filterData(original, projected, context,
						graphProjection);
			} else {
				return original;
			}
		}

		@Override
		public <T> boolean keepOriginal(T original,
				GraphProjectionContext context) {
			if (original instanceof Collection || original instanceof Map) {
				return false;
			} else {
				byClass.add(original.getClass(), original);
				return true;
			}
		}
	}

	class PermissibleFieldFilterH extends PermissibleFieldFilter {
		@Override
		public Boolean permitClass(Class clazz) {
			if (classFilter != null) {
				return classFilter.test(clazz);
			}
			if (!Entity.class.isAssignableFrom(clazz)) {
				return true;
			}
			clazz = Mvcc.resolveEntityClass(clazz);
			if (permittedClasses.size() > 0
					&& !permittedClasses.contains(clazz)) {
				return false;
			}
			if (forbiddenClasses.size() > 0
					&& forbiddenClasses.contains(clazz)) {
				return false;
			}
			return super.permitClass(clazz);
		}
	}
}
