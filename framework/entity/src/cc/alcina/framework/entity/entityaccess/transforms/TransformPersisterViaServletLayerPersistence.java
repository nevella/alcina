package cc.alcina.framework.entity.entityaccess.transforms;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface TransformPersisterViaServletLayerPersistence {
	public static TransformPersisterViaServletLayerPersistence get() {
		return Registry
				.impl(TransformPersisterViaServletLayerPersistence.class);
	}

	public void persistTransforms(boolean currentUser);
}