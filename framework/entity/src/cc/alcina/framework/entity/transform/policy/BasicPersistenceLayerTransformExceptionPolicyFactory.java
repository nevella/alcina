package cc.alcina.framework.entity.transform.policy;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

@Registration.Singleton(PersistenceLayerTransformExceptionPolicyFactory.class)
public class BasicPersistenceLayerTransformExceptionPolicyFactory
		implements PersistenceLayerTransformExceptionPolicyFactory {
	public PersistenceLayerTransformExceptionPolicy getPolicy(
			TransformPersistenceToken token, boolean forOfflineTransforms) {
		AbstractPersistenceLayerTransformExceptionPolicy policy = forOfflineTransforms
				? new IgnoreMissingPersistenceLayerTransformExceptionPolicy()
				: new BasicPersistenceLayerTransformExceptionPolicy();
		policy.setTransformPersistenceToken(token);
		return policy;
	}
}
