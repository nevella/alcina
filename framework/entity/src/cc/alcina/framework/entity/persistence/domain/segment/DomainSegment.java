package cc.alcina.framework.entity.persistence.domain.segment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.ValueContainer;
import cc.alcina.framework.entity.projection.CollectionProjectionFilterWithCache;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;

/**
 * <p>
 * A json-serializable representation of a domain segment
 */
public class DomainSegment {
	public static class SegmentEntity {
		transient Class<? extends Entity> entityClass;

		public long id;

		public long lastModificationTime;

		public ValueContainer[] values;

		public SegmentEntity() {
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s", entityClass.getSimpleName(),
					CommonUtils.padEight((int) id));
		}

		public SegmentEntity(Entity entity, ValueMapper mapper) {
			id = entity.getId();
			entityClass = entity.entityClass();
			Date lastModificationDate = ((VersionableEntity) entity)
					.getLastModificationDate();
			lastModificationTime = lastModificationDate == null ? 0L
					: lastModificationDate.getTime();
			mapper.setProperties(entity, this);
		}

		public SegmentEntity toMemberRef() {
			SegmentEntity result = new SegmentEntity();
			result.id = id;
			result.lastModificationTime = lastModificationTime;
			return result;
		}

		public EntityLocator toLocator() {
			return new EntityLocator(entityClass, id, 0L);
		}
	}

	public static class SegmentCollection {
		public Class<? extends Entity> entityClass;

		public List<SegmentEntity> segmentEntities = new ArrayList<>();

		transient Map<Long, SegmentEntity> idToEntity = new LinkedHashMap<>();

		public SegmentCollection() {
		}

		public SegmentCollection(Class<? extends Entity> entityClass) {
			this.entityClass = entityClass;
		}

		public SegmentCollection toMemberRefs() {
			SegmentCollection result = new SegmentCollection();
			result.entityClass = entityClass;
			result.segmentEntities = segmentEntities.stream()
					.map(SegmentEntity::toMemberRef)
					.collect(Collectors.toList());
			return result;
		}

		public void add(SegmentEntity entity) {
			segmentEntities.add(entity);
			index(entity);
		}

		void reindex() {
			idToEntity.clear();
			segmentEntities.forEach(this::index);
		}

		void indexToList() {
			segmentEntities = idToEntity.values().stream()
					.collect(Collectors.toList());
		}

		void index(SegmentEntity entity) {
			entity.entityClass = entityClass;
			idToEntity.put(entity.id, entity);
		}

		void applyIndexRemovals() {
			Iterator<SegmentEntity> itr = segmentEntities.iterator();
			while (itr.hasNext()) {
				SegmentEntity entity = itr.next();
				if (!idToEntity.containsKey(entity.id)) {
					itr.remove();
				}
			}
		}

		void remove(long id) {
			idToEntity.remove(id);
		}
	}

	/**
	 * Models the server-side segment entity projection
	 */
	public static class ProjectionFilter implements GraphProjectionFieldFilter {
		public static class Rule {
			boolean allow = true;

			Class clazz;

			String fieldName;

			public Rule() {
			}

			public Rule(Class clazz) {
				this.clazz = clazz;
			}

			public Rule withAllow(boolean allow) {
				this.allow = allow;
				return this;
			}

			public Rule withFieldName(PropertyEnum property) {
				this.fieldName = property.name();
				return this;
			}

			public Rule withFieldName(String fieldName) {
				this.fieldName = fieldName;
				return this;
			}

			public boolean deny(Class clazz) {
				if (allow) {
					return false;
				}
				return clazz == this.clazz;
			}

			boolean allow(Class clazz) {
				if (!allow) {
					return false;
				}
				return clazz == this.clazz;
			}

			boolean deny(Field field) {
				return field.getDeclaringClass() == clazz
						&& field.getName().equals(fieldName);
			}
		}

		transient List<Rule> rules = new ArrayList<>();

		public Rule addRule(Class clazz) {
			Rule rule = new Rule(clazz);
			rules.add(rule);
			return rule;
		}

		@Override
		public Boolean permitClass(Class clazz) {
			if (!Entity.class.isAssignableFrom(clazz)) {
				return true;
			}
			Class entityClass = Domain.resolveEntityClass(clazz);
			if (rules.stream().anyMatch(rule -> rule.deny(entityClass))) {
				return false;
			}
			return rules.stream().anyMatch(rule -> rule.allow(entityClass));
		}

		@Override
		public boolean permitField(Field field,
				Set<Field> perObjectPermissionFields, Class clazz) {
			if (rules.stream().anyMatch(rule -> rule.deny(field))) {
				return false;
			}
			return true;
		}

		@Override
		public boolean permitTransient(Field field) {
			return false;
		}
	}

	/**
	 * Models the definition of the segment - the roots and the filter. The
	 * 'getters' are named provide to avoid serialization
	 * 
	 */
	@Bean(PropertySource.FIELDS)
	public interface Definition {
		Stream<? extends Entity> provideRoots();

		void configureLocal();

		ProjectionFilter provideFieldFilter();

		default CollectionProjectionFilterWithCache provideDataFilter() {
			return new CollectionProjectionFilterWithCache();
		}

		default String name() {
			return NestedName.get(this);
		}

		// interface toString, basically
		default String asString() {
			return name();
		}

		/**
		 * This will normally be at least the {@link ClassRef}, {@link IUser}
		 * and {@link IGroup} implementations for the domain
		 */
		Set<Class<? extends Entity>> providePassthroughClasses();
	}

	class Lookup {
		Map<Class<? extends Entity>, SegmentCollection> entityCollection;

		public List<ValueContainer[]> getValues(Class<? extends Entity> clazz) {
			SegmentCollection segmentCollection = entityCollection.get(clazz);
			if (segmentCollection == null) {
				return List.of();
			} else {
				return segmentCollection.segmentEntities.stream()
						.map(e -> e.values).toList();
			}
		}

		Lookup() {
			entityCollection = collections.stream()
					.collect(AlcinaCollectors.toKeyMap(sc -> sc.entityClass));
			collections.forEach(SegmentCollection::reindex);
		}

		void indexToList() {
			collections.forEach(SegmentCollection::indexToList);
		}

		void remove(EntityLocator locator) {
			Class<? extends Entity> entityClass = locator.clazz;
			SegmentCollection segmentCollection = entityCollection
					.get(entityClass);
			if (segmentCollection != null) {
				segmentCollection.remove(locator.id);
			}
		}

		void add(SegmentEntity entity) {
			Class<? extends Entity> entityClass = entity.entityClass;
			SegmentCollection segmentCollection = entityCollection
					.get(entityClass);
			if (segmentCollection == null) {
				segmentCollection = new SegmentCollection(entityClass);
				entityCollection.put(entity.entityClass, segmentCollection);
				collections.add(segmentCollection);
			}
			segmentCollection.add(entity);
		}

		Stream<? extends SegmentEntity> allValues() {
			return entityCollection.values().stream()
					.flatMap(sc -> sc.idToEntity.values().stream());
		}

		void applySyncReceiverEntity(SegmentEntity syncReceiverEntity) {
			SegmentEntity resultEntity = get(syncReceiverEntity);
			if (resultEntity == null) {
				deleted.add(syncReceiverEntity.toLocator());
			} else if (resultEntity.lastModificationTime >= syncReceiverEntity.lastModificationTime) {
				entityCollection.get(syncReceiverEntity.entityClass).idToEntity
						.remove(syncReceiverEntity.id);
			}
		}

		SegmentEntity get(SegmentEntity existing) {
			SegmentCollection segmentCollection = entityCollection
					.get(existing.entityClass);
			if (segmentCollection == null) {
				return null;
			} else {
				return segmentCollection.idToEntity.get(existing.id);
			}
		}

		void applyIndexRemovals() {
			int before = entitySize();
			collections.forEach(SegmentCollection::applyIndexRemovals);
			int after = entitySize();
			logger.info(
					"Removing unchanged entities: {} total entities -> {} changed entities",
					before, after);
		}
	}

	transient static Logger logger = LoggerFactory
			.getLogger(DomainSegment.class);

	List<SegmentCollection> collections = new ArrayList<>();

	List<EntityLocator> deleted = new ArrayList<>();

	public DomainSegment toLocalState() {
		DomainSegment result = new DomainSegment();
		result.collections = collections.stream()
				.map(SegmentCollection::toMemberRefs)
				.collect(Collectors.toList());
		return result;
	}

	@Override
	public String toString() {
		return Ax.format("%s collections - %s entities - %s removals",
				collections.size(), entitySize(), deleted.size());
	}

	public void merge(DomainSegment remoteUpdates) {
		Lookup remoteLookup = remoteUpdates.new Lookup();
		Lookup toLookup = new Lookup();
		remoteLookup.allValues().forEach(toLookup::add);
		remoteUpdates.deleted.forEach(toLookup::remove);
		toLookup.indexToList();
	}

	public void addCache(DetachedEntityCache cache) {
		Lookup lookup = new Lookup();
		ValueMapper mapper = new ValueMapper();
		cache.allValues().stream().map(e -> new SegmentEntity(e, mapper))
				.forEach(lookup::add);
	}

	public void filterExisting(DomainSegment syncReceiverState) {
		Lookup resultLookup = new Lookup();
		Lookup syncReceiverLookup = syncReceiverState.new Lookup();
		syncReceiverLookup.allValues()
				.forEach(resultLookup::applySyncReceiverEntity);
		resultLookup.applyIndexRemovals();
	}

	int entitySize() {
		return collections.stream().collect(
				Collectors.summingInt(sc -> sc.segmentEntities.size()));
	}
}
