package cc.alcina.framework.entity.domaintransform;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;

public interface RemoteTransformPersister {
    DomainTransformLayerWrapper submitAndHandleTransformsWritableStore(
            TransformPersistenceToken persistenceToken)
            throws DomainTransformRequestException;
}