package cc.alcina.framework.entity.persistence.domain.segment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;

/**
 * <p>
 * A json-serializable representation of a domain segment
 */
@Bean(PropertySource.FIELDS)
public class DomainSegment {
	@TypeSerialization("entity")
	@Bean(PropertySource.FIELDS)
	public static class SegmentEntity {
		public long id;

		public long lastModificationTime;

		public List<DomainSegmentProperty> properties;

		public SegmentEntity toMemberRef() {
			SegmentEntity result = new SegmentEntity();
			result.id = lastModificationTime;
			result.lastModificationTime = lastModificationTime;
			return result;
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class SegmentCollection {
		public Class<? extends Entity> entityClass;

		public List<SegmentEntity> segmentEntities;

		public SegmentCollection toMemberRefs() {
			SegmentCollection result = new SegmentCollection();
			result.entityClass = entityClass;
			result.segmentEntities = segmentEntities.stream()
					.map(SegmentEntity::toMemberRef).toList();
			return result;
		}
	}

	List<SegmentCollection> collections = new ArrayList<>();

	public DomainSegment toLocalState() {
		DomainSegment result = new DomainSegment();
		result.collections = collections.stream()
				.map(SegmentCollection::toMemberRefs).toList();
		return result;
	}

	transient Map<Class<? extends Entity>, SegmentEntity> lookup = new ConcurrentHashMap<>();

	/**
	 * Models the server-side segment entity projection
	 */
	public abstract static class ProjectionFilter
			implements GraphProjectionFieldFilter {
	}

	/**
	 * Models the definition of the segment - the roots and the filter
	 * 
	 */
	public interface Definition {
		Stream<Entity> getRoots();

		Class<? extends ProjectionFilter> getProjectionFilter();

		default String name() {
			return getClass().getSimpleName();
		}
	}

	public void merge(DomainSegment remoteUpdates) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'merge'");
	}
}
