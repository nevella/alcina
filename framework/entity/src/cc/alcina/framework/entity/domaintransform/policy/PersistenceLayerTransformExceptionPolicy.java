package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public interface PersistenceLayerTransformExceptionPolicy {

	public TransformExceptionAction getActionForException(
			DomainTransformException exception,
			TransformPersistenceToken persistenceToken);

	public enum TransformExceptionAction {
		THROW, RESOLVE
	}
}
