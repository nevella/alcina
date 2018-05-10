package cc.alcina.framework.servlet;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.logic.EntityLayerPersister;

@RegistryLocation(registryPoint = EntityLayerPersister.class, implementationType = ImplementationType.SINGLETON)
public class EntityLayerPersisterImpl implements EntityLayerPersister {
	@Override
	public void commit() {
		Sx.commit();
	}

	@Override
	public DomainTransformLayerWrapper pushTransforms(String tag,
			boolean asRoot, boolean returnResponse) {
		return ServletLayerUtils.pushTransforms(tag, asRoot, returnResponse);
	}
}
