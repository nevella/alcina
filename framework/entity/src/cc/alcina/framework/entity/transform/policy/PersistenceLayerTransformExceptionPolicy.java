package cc.alcina.framework.entity.transform.policy;

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

public interface PersistenceLayerTransformExceptionPolicy extends Serializable {
	public void checkVersion(Entity obj, DomainTransformEvent event)
			throws DomainTransformException;

	public TransformExceptionAction getActionForException(
			DomainTransformException exception,
			TransformPersistenceToken persistenceToken);

	public boolean ignoreClientAuthMismatch(
			ClientInstance persistentClientInstance,
			DomainTransformRequest request);

	public boolean precreateMissingEntities();

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
}
