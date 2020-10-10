package cc.alcina.framework.entity.transform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;

public class BasicPersistenceLayerTransformExceptionPolicy
		extends AbstractPersistenceLayerTransformExceptionPolicy {
	private static final int TOO_MANY_EXCEPTIONS = 30;

	public TransformExceptionAction getActionForException(
			DomainTransformException exception,
			TransformPersistenceToken persistenceToken) {
		if (persistenceToken.getTransformExceptions()
				.size() < TOO_MANY_EXCEPTIONS) {
			return TransformExceptionAction.RESOLVE;
		}
		exception.setType(DomainTransformExceptionType.TOO_MANY_EXCEPTIONS);
		return TransformExceptionAction.THROW;
	}

	@Override
	public boolean ignoreClientAuthMismatch(
			ClientInstance persistentClientInstance,
			DomainTransformRequest request) {
		return false;
	}

	@Override
	public boolean precreateMissingEntities() {
		return false;
	}
}
