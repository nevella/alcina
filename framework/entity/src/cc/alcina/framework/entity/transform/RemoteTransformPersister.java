package cc.alcina.framework.entity.transform;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;

public interface RemoteTransformPersister {
	DomainTransformLayerWrapper submitAndHandleTransformsRemoteStore(
			TransformPersistenceToken persistenceToken)
			throws DomainTransformRequestException;
}