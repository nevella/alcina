package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.entity.domaintransform.TransformConflicts;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public abstract class AbstractPersistenceLayerTransformExceptionPolicy
		implements PersistenceLayerTransformExceptionPolicy {
	private boolean checkVersion;

	private TransformPersistenceToken transformPersistenceToken;

	private TransformConflicts transformConflicts;

	public AbstractPersistenceLayerTransformExceptionPolicy() {
	}

	@Override
	public void checkVersion(HasIdAndLocalId obj, DomainTransformEvent event)
			throws DomainTransformException {
		if (checkVersion) {
			if(transformConflicts==null){
				transformConflicts=new TransformConflicts();
				transformConflicts.setTransformPersistenceToken(transformPersistenceToken);
			}
			transformConflicts.checkVersion(obj, event);
		}
	}

	public boolean isCheckVersion() {
		return this.checkVersion;
	}

	public void setCheckVersion(boolean checkVersion) {
		this.checkVersion = checkVersion;
	}

	public TransformPersistenceToken getTransformPersistenceToken() {
		return this.transformPersistenceToken;
	}

	public void setTransformPersistenceToken(
			TransformPersistenceToken transformPersistenceToken) {
		this.transformPersistenceToken = transformPersistenceToken;
	}
}