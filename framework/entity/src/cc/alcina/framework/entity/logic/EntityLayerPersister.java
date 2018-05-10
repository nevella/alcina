package cc.alcina.framework.entity.logic;

import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;

public interface EntityLayerPersister {
	void commit();
	DomainTransformLayerWrapper pushTransforms(String tag,
            boolean asRoot, boolean returnResponse) ;
}
