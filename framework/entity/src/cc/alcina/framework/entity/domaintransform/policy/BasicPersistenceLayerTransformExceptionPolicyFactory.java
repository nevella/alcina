package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
@RegistryLocation(registryPoint = PersistenceLayerTransformExceptionPolicyFactory.class, implementationType = ImplementationType.SINGLETON)
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
