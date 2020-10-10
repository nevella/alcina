package cc.alcina.framework.entity.transform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;

public class IgnoreMissingPersistenceLayerTransformExceptionPolicy
		extends AbstractPersistenceLayerTransformExceptionPolicy {
	private static final int TOO_MANY_EXCEPTIONS = 30;

	public TransformExceptionAction getActionForException(
			DomainTransformException exception,
			TransformPersistenceToken persistenceToken) {
		switch (exception.getType()) {
		case SOURCE_ENTITY_NOT_FOUND:
		case TARGET_ENTITY_NOT_FOUND:
		case INTROSPECTION_EXCEPTION:
			return TransformExceptionAction.IGNORE_AND_WARN;
		default:
			break;
		}
		if (persistenceToken.getTransformExceptions()
				.size() < tooManyExceptions()) {
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

	public int tooManyExceptions() {
		return TOO_MANY_EXCEPTIONS;
	}
}
