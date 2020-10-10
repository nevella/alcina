package cc.alcina.framework.entity.transform.policy;

import cc.alcina.framework.entity.transform.TransformPersistenceToken;

public interface PersistenceLayerTransformExceptionPolicyFactory {
	PersistenceLayerTransformExceptionPolicy getPolicy(
			TransformPersistenceToken token, boolean forOfflineTransforms);
}
