package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;

public class BasicPersistenceLayerTransformExceptionPolicyFactory implements
		PersistenceLayerTransformExceptionPolicyFactory {
	public PersistenceLayerTransformExceptionPolicy getPolicy(
			DomainTransformRequest transformRequest) {
		return new BasicPersistenceLayerTransformExceptionPolicy();
	}
}
