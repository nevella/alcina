package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.entity.entityaccess.TransformPersisterViaServletLayerPersistence;

public class TransformPersisterViaServletLayerPersistenceStd
		implements TransformPersisterViaServletLayerPersistence {
	@Override
	public void persistTransforms(boolean currentUser) {
		if (currentUser) {
			ServletLayerTransforms.pushTransformsAsCurrentUser();
		} else {
			ServletLayerTransforms.pushTransformsAsRoot();
		}
	}
}