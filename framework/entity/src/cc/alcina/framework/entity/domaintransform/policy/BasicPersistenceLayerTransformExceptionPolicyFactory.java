package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public class BasicPersistenceLayerTransformExceptionPolicyFactory implements
		PersistenceLayerTransformExceptionPolicyFactory {
	public PersistenceLayerTransformExceptionPolicy getPolicy(
			DomainTransformRequest transformRequest) {
		return new BasicPersistenceLayerTransformExceptionPolicy();
	}
}
