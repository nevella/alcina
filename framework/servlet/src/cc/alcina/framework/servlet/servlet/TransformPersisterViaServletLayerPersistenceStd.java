package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.entityaccess.transforms.TransformCommit;
import cc.alcina.framework.entity.entityaccess.transforms.TransformPersisterViaServletLayerPersistence;

@RegistryLocation(registryPoint = TransformPersisterViaServletLayerPersistence.class, implementationType = ImplementationType.SINGLETON)
public class TransformPersisterViaServletLayerPersistenceStd
		implements TransformPersisterViaServletLayerPersistence {
	@Override
	public void persistTransforms(boolean currentUser) {
		if (currentUser) {
			TransformCommit.pushTransformsAsCurrentUser();
		} else {
			TransformCommit.pushTransformsAsRoot();
		}
	}
}