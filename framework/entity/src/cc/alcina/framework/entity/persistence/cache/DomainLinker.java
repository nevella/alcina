package cc.alcina.framework.entity.persistence.cache;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.JPAImplementation;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter;
import cc.alcina.framework.entity.util.MethodContext;

public class DomainLinker<E extends Entity> {
	public static <T> T linkToDomain(T t) {
		GraphProjectionDataFilter dataFilter = Registry
				.impl(JPAImplementation.class)
				.getResolvingFilter(null, null, true);
		return GraphProjections.defaultProjections()
				.fieldFilter(Registry.impl(PermissibleFieldFilter.class))
				.dataFilter(dataFilter).project(t);
	}

	private EntityManager em;

	private Class<E> clazz;

	private String alias;

	private List<Mapping> mappings;

	private DetachedEntityCache cache;

	private DomainLinker parent;

	private int mappingOffset;

	private Multiset<Class<? extends Entity>, Set<Long>> queried;

	private List<Runnable> resolveTasks;

	Logger logger = LoggerFactory.getLogger(getClass());

	private LinkerFilter fieldFilter;

	private List<Field> fields;

	public DomainLinker(EntityManager em, Class<E> clazz, String alias,
			LinkerFilter fieldFilter) {
		this(em, clazz, alias, fieldFilter, null);
	}

	private DomainLinker(EntityManager em, Class<E> clazz, String alias,
			LinkerFilter fieldFilter, DomainLinker parent) {
		this.em = em;
		this.clazz = clazz;
		this.alias = alias;
		this.parent = parent;
		if (fieldFilter == null) {
			fieldFilter = Registry.impl(LinkerFilter.class);
		}
		this.fieldFilter = fieldFilter;
		if (parent == null) {
			cache = new DetachedEntityCache();
			queried = new Multiset<>();
			resolveTasks = new ArrayList<>();
		}
	}

	public String createObjectRefSelect() {
		fields = DomainStore.writableStore().getFields(clazz).stream()
				.filter(f -> {
					PropertyDescriptor pd = SEUtilities
							.getPropertyDescriptorByName(clazz, f.getName());
					return !(pd == null || pd.getReadMethod() == null
							|| pd.getReadMethod()
									.getAnnotation(Transient.class) != null);
				}).collect(Collectors.toList());
		this.mappings = fields.stream()
				.filter(f -> Entity.class.isAssignableFrom(f.getType())
						|| Set.class.isAssignableFrom(f.getType()))
				.filter(fieldFilter).map(Mapping::new)
				.collect(Collectors.toList());
		String clause = mappings.stream().filter(m -> !m.isOneToMany())
				.map(Mapping::toSelectClause).collect(Collectors.joining(", "));
		if (clause.length() > 0) {
			clause = ", " + clause;
		} else {
			if (parent != null) {
				// add a dummy col
				return Ax.format(", %s.id", alias);
			} else {
				throw new RuntimeException("No outgoing object refs");
			}
		}
		return clause;
	}

	public List<E> linkAndDetach(EntityManager em, String eql) {
		MethodContext methodContext = MethodContext.instance();
		if (TransformManager.get().getTransforms().isEmpty()) {
			methodContext.withExecuteOutsideTransaction();
		}
		List<Object[]> objs = methodContext
				.call(() -> em.createQuery(eql).getResultList());
		return linkAndDetach(objs);
	}

	private DetachedEntityCache cache() {
		return parent == null ? cache : parent.cache();
	}

	private void link(Set<Long> ids, String linkFieldName) {
		Set<Long> clazzQueried = queried().get(clazz);
		if (clazzQueried != null) {
			ids.removeAll(clazzQueried);
		}
		if (ids.isEmpty()) {
			return;
		}
		String select = Ax.format(
				"select distinct %s %s from %s %s where %s.%s in %s", alias,
				createObjectRefSelect(), clazz.getSimpleName(), alias, alias,
				linkFieldName, EntityPersistenceHelper.toInClause(ids));
		String metricKey = null;
		if (!AppPersistenceBase.isTestServer() || true) {
			logger.info("Resolve refs query :: {} :: {} ids",
					clazz.getSimpleName(), ids.size());
			metricKey = metricKey();
		}
		long start = System.currentTimeMillis();
		List<Object[]> resultList = MethodContext.instance()
				.withMetricKey(metricKey)
				.call(() -> em.createQuery(select).getResultList());
		long end = System.currentTimeMillis();
		if (end - start > 1000) {
			logger.info("Resolve refs query time debug:: {} :: {} ids\n{}",
					clazz.getSimpleName(), ids.size(), select);
		}
		queried().addCollection(clazz, ids);
		linkAndDetach(resultList);
	}

	private List<E> linkAndDetach(List<Object[]> objs) {
		if (objs.isEmpty()) {
			return new ArrayList<>();
		}
		mappingOffset = (int) (objs.get(0).length
				- mappings.stream().filter(m -> !m.isOneToMany()).count());
		/*
		 * Construct our detached copies and put them in the graph first, so
		 * that subLinkers can find them
		 */
		List<E> result = new ArrayList<>();
		List<String> ignorePropertyNames = fields.stream()
				.filter(f -> Set.class.isAssignableFrom(f.getType()))
				.map(Field::getName).collect(Collectors.toList());
		for (Object[] array : objs) {
			E attached = (E) array[0];
			E detached = Reflections.classLookup().newInstance(clazz);
			result.add(detached);
			ResourceUtilities.copyBeanProperties(attached, detached, null, true,
					ignorePropertyNames);
			cache().put(detached);
			// only needed for top-level
			queried().add(clazz, detached.getId());
			for (Mapping mapping : mappings) {
				mapping.apply(array, detached);
			}
		}
		mappings.forEach(Mapping::resolve);
		if (parent == null) {
			resolveTasks.forEach(Runnable::run);
		}
		return result;
	}

	private String metricKey() {
		String prefix = parent == null ? "" : parent.metricKey() + ".";
		return prefix + clazz.getSimpleName();
	}

	private Multiset<Class<? extends Entity>, Set<Long>> queried() {
		return parent == null ? queried : parent.queried();
	}

	List<Runnable> resolveTasks() {
		return parent == null ? resolveTasks : parent.resolveTasks();
	}

	public static abstract class LinkerFilter implements Predicate<Field> {
	}

	public static abstract class LinkerFilterEntity extends LinkerFilter {
		@Override
		public boolean test(Field t) {
			if (Entity.class.isAssignableFrom(t.getType())) {
				return true;
			}
			return false;
		}
	}

	private class Mapping {
		private Field field;

		private DomainLinker subLinker;

		private int offset = -1;

		private Map<E, Long> byEntity;

		private Field associatedField;

		Mapping(Field field) {
			this.field = field;
			if (!isDomainClass() || isOneToMany()) {
				byEntity = new LinkedHashMap<>();
			}
			Association association = SEUtilities
					.getPropertyDescriptorByName(clazz, field.getName())
					.getReadMethod().getAnnotation(Association.class);
			if (association != null) {
				try {
					associatedField = DomainStore.writableStore().getField(
							association.implementationClass(),
							association.propertyName());
					if (!fieldFilter.test(associatedField)) {
						associatedField = null;
					}
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}

		@Override
		public String toString() {
			return Ax.format("%s.%s", field.getDeclaringClass().getSimpleName(),
					field.getName());
		}

		private Class<? extends Entity> getType() {
			if (GraphProjection.isGenericEntityType(field)) {
				Type pt = GraphProjection.getGenericType(field);
				if (pt instanceof ParameterizedType) {
					Type genericType = ((ParameterizedType) pt)
							.getActualTypeArguments()[0];
					if (genericType instanceof Class) {
						return (Class<? extends Entity>) genericType;
					}
				}
				throw new RuntimeException();
			}
			return (Class<? extends Entity>) field.getType();
		}

		private boolean isOneToMany() {
			return Set.class.isAssignableFrom(field.getType());
		}

		private int offset() {
			if (offset == -1) {
				offset = mappings.indexOf(this);
			}
			return offset;
		}

		void apply(Object[] array, E detached) {
			try {
				apply0(array, detached);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		void apply0(Object[] array, E detached) throws Exception {
			if (isOneToMany()) {
				byEntity.put(detached, null);
				field.set(detached, new LiSet<>());
				return;
			}
			Long id = (Long) array[offset() + mappingOffset];
			if (id != null) {
				if (isDomainClass()) {
					field.set(detached, Domain.find(getType(), id));
					return;
				} else {
					byEntity.put(detached, id);
				}
			}
		}

		boolean isDomainClass() {
			return DomainStore.writableStore().isCached(getType());
		}

		void resolve() {
			if (isDomainClass()) {
				return;
			}
			if (!byEntity.isEmpty()) {
				subLinker = new DomainLinker<>(em, getType(), "sub",
						fieldFilter, DomainLinker.this);
				if (isOneToMany()) {
					subLinker.link(
							byEntity.keySet().stream()
									.collect(EntityHelper.toIdSet()),
							associatedField.getName());
				} else {
					subLinker.link(byEntity.values().stream()
							.collect(Collectors.toSet()), "id");
					/*
					 * run the resolve tasks at the end since we got noooo idea
					 * what order things will be called in .... shades of
					 * DomainStore.LazyLoader (but dissimilar enough to not
					 * justify shared code)
					 */
					resolveTasks().add(() -> {
						byEntity.entrySet().forEach(e -> {
							try {
								E detached = e.getKey();
								Entity target = cache().get(getType(),
										e.getValue());
								field.set(detached, target);
								if (associatedField != null
										&& Set.class.isAssignableFrom(
												associatedField.getType())) {
									Set set = (Set) associatedField.get(target);
									set.add(detached);
								}
							} catch (Exception e2) {
								throw new WrappedRuntimeException(e2);
							}
						});
					});
				}
			}
		}

		String toSelectClause() {
			return Ax.format("%s.%s.id", alias, field.getName());
		}
	}
}
