package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.entity.entityaccess.TransformPersisterViaServletLayerPersistence;
import cc.alcina.framework.servlet.ServletLayerUtils;

public class TransformPersisterViaServletLayerPersistenceStd
		implements TransformPersisterViaServletLayerPersistence {
	@Override
	public void persistTransforms() {
		ServletLayerUtils.pushTransformsAsRoot();
	}
}