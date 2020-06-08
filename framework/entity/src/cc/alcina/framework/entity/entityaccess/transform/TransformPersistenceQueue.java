package cc.alcina.framework.entity.entityaccess.transform;

import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public interface TransformPersistenceQueue {
	DomainTransformLayerWrapper
			submit(TransformPersistenceToken persistenceToken);
}
