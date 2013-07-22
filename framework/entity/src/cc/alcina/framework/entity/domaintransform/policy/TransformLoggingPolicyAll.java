package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint=TransformLoggingPolicy.class,implementationType=ImplementationType.INSTANCE)
public class TransformLoggingPolicyAll implements TransformLoggingPolicy{
	@Override
	public boolean shouldPersist(DomainTransformEvent dte){
		return true;
	}
}
