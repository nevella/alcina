package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public interface PersistenceLayerTransformExceptionPolicyFactory {

	PersistenceLayerTransformExceptionPolicy getPolicy(
			TransformPersistenceToken token, boolean forOfflineTransforms);
}
