package cc.alcina.framework.entity.persistence.transform;

import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

public interface TransformPersistenceQueue {
	DomainTransformLayerWrapper
			submit(TransformPersistenceToken persistenceToken);
}
