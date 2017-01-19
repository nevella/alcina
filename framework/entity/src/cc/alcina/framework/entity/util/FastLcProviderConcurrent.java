package cc.alcina.framework.entity.util;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CachingMap.CachingLcMap;
import cc.alcina.framework.common.client.util.FastLcProvider;
import cc.alcina.framework.entity.util.CachingConcurrentMap.CachingConcurrentLcMap;

@RegistryLocation(registryPoint = FastLcProvider.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
@ClientInstantiable
public class FastLcProviderConcurrent extends FastLcProvider{
	private CachingConcurrentLcMap map = new CachingConcurrentLcMap();

	public String lc(String string) {
		return map.get(string);
	}
}