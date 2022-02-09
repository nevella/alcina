package cc.alcina.framework.entity.util;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.FastLcProvider;
import cc.alcina.framework.entity.util.CachingConcurrentMap.CachingConcurrentLcMap;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@RegistryLocation(registryPoint = FastLcProvider.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
@ClientInstantiable
@Registration(value = FastLcProvider.class, priority = Registration.Priority.PREFERRED_LIBRARY)
public class FastLcProviderConcurrent extends FastLcProvider {

    private CachingConcurrentLcMap map = new CachingConcurrentLcMap();

    public String lc(String string) {
        return string == null ? null : map.get(string);
    }
}
