package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = KnownsRegistrar.class, implementationType = ImplementationType.SINGLETON)
public abstract class KnownsRegistrar {

	public abstract void register() ;
}