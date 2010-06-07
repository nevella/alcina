package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public interface TransformPersistenceQueue {

	DomainTransformLayerWrapper submit(
			TransformPersistenceToken persistenceToken) ;
}
