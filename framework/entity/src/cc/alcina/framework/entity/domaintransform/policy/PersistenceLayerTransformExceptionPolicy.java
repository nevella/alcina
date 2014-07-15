package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public interface PersistenceLayerTransformExceptionPolicy {
	public TransformExceptionAction getActionForException(
			DomainTransformException exception,
			TransformPersistenceToken persistenceToken);
	
	public enum TransformExceptionAction {
		THROW, RESOLVE, IGNORE_AND_WARN {
			@Override
			public boolean ignoreable() {
				return true;
			}
		},
		IGNORE_SILENT {
			@Override
			public boolean ignoreable() {
				return true;
			}
		};
		public boolean ignoreable() {
			return false;
		}
	}

	public boolean precreateMissingEntities();
	
	public void checkVersion(HasIdAndLocalId obj, DomainTransformEvent event)
			throws DomainTransformException;

	public boolean ignoreClientAuthMismatch(
			ClientInstance persistentClientInstance, DomainTransformRequest request);
	
	
}
