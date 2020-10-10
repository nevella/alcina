package cc.alcina.framework.entity.transform.policy;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.entity.transform.TransformConflicts;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

public abstract class AbstractPersistenceLayerTransformExceptionPolicy
		implements PersistenceLayerTransformExceptionPolicy {
	private boolean checkVersion;

	private TransformPersistenceToken transformPersistenceToken;

	private TransformConflicts transformConflicts;

	public AbstractPersistenceLayerTransformExceptionPolicy() {
	}

	@Override
	public void checkVersion(Entity obj, DomainTransformEvent event)
			throws DomainTransformException {
		if (checkVersion) {
			if (transformConflicts == null) {
				transformConflicts = new TransformConflicts();
				transformConflicts.setTransformPersistenceToken(
						transformPersistenceToken);
			}
			transformConflicts.checkVersion(obj, event);
		}
	}

	public TransformPersistenceToken getTransformPersistenceToken() {
		return this.transformPersistenceToken;
	}

	public boolean isCheckVersion() {
		return this.checkVersion;
	}

	public void setCheckVersion(boolean checkVersion) {
		this.checkVersion = checkVersion;
	}

	public void setTransformPersistenceToken(
			TransformPersistenceToken transformPersistenceToken) {
		this.transformPersistenceToken = transformPersistenceToken;
	}
}