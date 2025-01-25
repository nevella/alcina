package cc.alcina.framework.entity.persistence.domain.segment;

import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.EntityValuesMapper;
import cc.alcina.framework.entity.persistence.domain.segment.DomainSegment.SegmentEntity;

class ValueMapper {
	ConcurrentHashMap<Class, PerClass> perClass = new ConcurrentHashMap<>();

	static class PerClass {
		Class<? extends Entity> entityClass;

		EntityValuesMapper entityValuesMapper;

		PerClass(Class<? extends Entity> entityClass) {
			this.entityClass = entityClass;
			entityValuesMapper = DomainStore.writableStore()
					.getEntityValuesMapper(entityClass);
		}

		void map(Entity entity, SegmentEntity segmentEntity) {
			segmentEntity.values = entityValuesMapper.getValues(entity);
		}
	}

	void setProperties(Entity entity, SegmentEntity segmentEntity) {
		perClass.computeIfAbsent(entity.entityClass(), PerClass::new)
				.map(entity, segmentEntity);
	}
}