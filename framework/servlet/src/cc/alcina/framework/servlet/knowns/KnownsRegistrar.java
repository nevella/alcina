package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;

@RegistryLocations({
        @RegistryLocation(registryPoint = KnownsRegistrar.class, implementationType = ImplementationType.SINGLETON),
        @RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class) })
public abstract class KnownsRegistrar {
    public abstract void register(KnownsPersistence persistence);
}