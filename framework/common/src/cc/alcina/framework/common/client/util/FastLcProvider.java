package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CachingMap.CachingLcMap;

@RegistryLocation(registryPoint = FastLcProvider.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
public class FastLcProvider {
	private CachingLcMap map = new CachingLcMap();

	public String lc(String string) {
		return map.get(string);
	}
}