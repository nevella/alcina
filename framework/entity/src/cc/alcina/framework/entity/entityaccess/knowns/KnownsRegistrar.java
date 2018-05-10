package cc.alcina.framework.entity.entityaccess.knowns;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = KnownsRegistrar.class, implementationType = ImplementationType.SINGLETON)
public abstract class KnownsRegistrar {

	public abstract void register() ;
}