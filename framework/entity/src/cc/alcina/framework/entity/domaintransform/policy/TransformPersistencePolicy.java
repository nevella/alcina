package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = TransformPersistencePolicy.class, implementationType = ImplementationType.INSTANCE)
public class TransformPersistencePolicy {
	public boolean shouldPersist(DomainTransformEvent event) {
		return true;
	}
}
