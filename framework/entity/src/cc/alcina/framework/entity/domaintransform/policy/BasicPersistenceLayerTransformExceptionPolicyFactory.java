package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public class BasicPersistenceLayerTransformExceptionPolicyFactory implements
		PersistenceLayerTransformExceptionPolicyFactory {
	public PersistenceLayerTransformExceptionPolicy getPolicy(
			TransformPersistenceToken token, boolean forOfflineTransforms) {
		AbstractPersistenceLayerTransformExceptionPolicy policy = forOfflineTransforms ? new IgnoreMissingPersistenceLayerTransformExceptionPolicy()
				: new BasicPersistenceLayerTransformExceptionPolicy();
		policy.setTransformPersistenceToken(token);
		return policy;
	}
}
