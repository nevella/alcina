package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public class IgnoreMissingPersistenceLayerTransformExceptionPolicy implements
		PersistenceLayerTransformExceptionPolicy {
	private static final int TOO_MANY_EXCEPTIONS = 30;

	public TransformExceptionAction getActionForException(
			DomainTransformException exception,
			TransformPersistenceToken persistenceToken) {
		switch (exception.getType()) {
		case SOURCE_ENTITY_NOT_FOUND:
		case TARGET_ENTITY_NOT_FOUND:
		case INTROSPECTION_EXCEPTION:
			return TransformExceptionAction.IGNORE_AND_WARN;
		}
		if (persistenceToken.getTransformExceptions().size() < tooManyExceptions()) {
			return TransformExceptionAction.RESOLVE;
		}
		exception.setType(DomainTransformExceptionType.TOO_MANY_EXCEPTIONS);
		return TransformExceptionAction.THROW;
	}

	public int tooManyExceptions() {
		return TOO_MANY_EXCEPTIONS;
	}

	@Override
	public boolean precreateMissingEntities() {
		return false;
	}
}
