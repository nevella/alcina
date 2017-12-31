package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public class ReconstituteDbTransformExceptionPolicy
		extends IgnoreMissingPersistenceLayerTransformExceptionPolicy {
	@Override
	public TransformExceptionAction getActionForException(
			DomainTransformException exception,
			TransformPersistenceToken persistenceToken) {
		if (exception
				.getType() == DomainTransformExceptionType.FK_CONSTRAINT_EXCEPTION) {
			return TransformExceptionAction.IGNORE_AND_WARN;
		}
		return super.getActionForException(exception, persistenceToken);
	}

	@Override
	public boolean precreateMissingEntities() {
		return true;
	}

	@Override
	public int tooManyExceptions() {
		return 9999;
	}
}