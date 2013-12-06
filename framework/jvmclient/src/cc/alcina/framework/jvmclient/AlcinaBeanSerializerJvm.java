package cc.alcina.framework.jvmclient;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;

@RegistryLocation(registryPoint = AlcinaBeanSerializer.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
@ClientInstantiable
public class AlcinaBeanSerializerJvm extends AlcinaBeanSerializerS {
}
