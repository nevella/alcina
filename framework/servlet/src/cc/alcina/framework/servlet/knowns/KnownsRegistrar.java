package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@RegistryLocations({ @RegistryLocation(registryPoint = KnownsRegistrar.class, implementationType = ImplementationType.SINGLETON), @RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class) })
@Registrations({ @Registration(value = KnownsRegistrar.class, implementation = Registration.Implementation.SINGLETON), @Registration(ClearStaticFieldsOnAppShutdown.class) })
public abstract class KnownsRegistrar {

    public abstract void register(KnownsPersistence persistence);
}
