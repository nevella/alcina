package cc.alcina.framework.entity.persistence.domain.segment;

import java.io.Serializable;
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
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.ValueContainer;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;

/**
 * <p>
 * A json-serializable representation of a domain segment
 */
public class DomainSegment implements Serializable {
	transient static Logger logger = LoggerFactory
			.getLogger(DomainSegment.class);

	public static class SegmentEntity implements Serializable {
		public SegmentEntity() {
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

		transient Class<? extends Entity> entityClass;

		public long id;

		public long lastModificationTime;

		public ValueContainer[] values;

		public SegmentEntity toMemberRef() {
			SegmentEntity result = new SegmentEntity();
			result.id = lastModificationTime;
			result.lastModificationTime = lastModificationTime;
			return result;
		}
	}

	public static class SegmentCollection implements Serializable {
		public SegmentCollection() {
		}

		public SegmentCollection(Class<? extends Entity> entityClass) {
			this.entityClass = entityClass;
		}

		public Class<? extends Entity> entityClass;

		public List<SegmentEntity> segmentEntities = new ArrayList<>();

		transient Map<Long, SegmentEntity> idToEntity = new LinkedHashMap<>();

		public SegmentCollection toMemberRefs() {
			SegmentCollection result = new SegmentCollection();
			result.entityClass = entityClass;
			result.segmentEntities = segmentEntities.stream()
					.map(SegmentEntity::toMemberRef).toList();
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
			segmentEntities = idToEntity.values().stream().toList();
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
	}

	List<SegmentCollection> collections = new ArrayList<>();

	public DomainSegment toLocalState() {
		DomainSegment result = new DomainSegment();
		result.collections = collections.stream()
				.map(SegmentCollection::toMemberRefs).toList();
		return result;
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
	 * Models the definition of the segment - the roots and the filter
	 * 
	 */
	@Bean(PropertySource.FIELDS)
	public interface Definition {
		@Property.Not
		Stream<? extends Entity> getRoots();

		void configureLocal();

		@Property.Not
		ProjectionFilter getProjectionFilter();

		default String name() {
			return getClass().getSimpleName();
		}

		// interface toString, basically
		default String asString() {
			return getClass().getSimpleName();
		}
	}

	@Override
	public String toString() {
		return Ax.format("%s collections - %s entities", collections.size(),
				entitySize());
	}

	int entitySize() {
		return collections.stream().collect(
				Collectors.summingInt(sc -> sc.segmentEntities.size()));
	}

	public void merge(DomainSegment remoteUpdates) {
		Lookup remoteLookup = remoteUpdates.new Lookup();
		Lookup toLookup = new Lookup();
		remoteLookup.allValues().forEach(toLookup::add);
		toLookup.indexToList();
	}

	class Lookup {
		Map<Class<? extends Entity>, SegmentCollection> entityCollection;

		Lookup() {
			entityCollection = collections.stream()
					.collect(AlcinaCollectors.toKeyMap(sc -> sc.entityClass));
			collections.forEach(SegmentCollection::reindex);
		}

		void indexToList() {
			collections.forEach(SegmentCollection::indexToList);
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

		void removeIfUnchanged(SegmentEntity existing) {
			SegmentEntity local = get(existing);
			if (local != null
					&& local.lastModificationTime >= existing.lastModificationTime) {
				entityCollection.get(existing.entityClass).idToEntity
						.remove(existing.id);
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
			logger.info("Index removals: {} -> {} entities", before, after);
		}
	}

	public void addCache(DetachedEntityCache cache) {
		Lookup lookup = new Lookup();
		ValueMapper mapper = new ValueMapper();
		cache.allValues().stream().map(e -> new SegmentEntity(e, mapper))
				.forEach(lookup::add);
	}

	public void filterExisting(DomainSegment localState) {
		Lookup toLookup = new Lookup();
		Lookup localLookup = localState.new Lookup();
		localLookup.allValues().forEach(toLookup::removeIfUnchanged);
		toLookup.applyIndexRemovals();
	}
}
