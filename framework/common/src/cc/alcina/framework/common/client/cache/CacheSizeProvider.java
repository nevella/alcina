package cc.alcina.framework.common.client.cache;

import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = CacheSizeProvider.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class CacheSizeProvider {
	public int size(String descriptorId) {
		return 100;
	}

	public void registerMap(String descriptorId, Map map) {
	}
	
	public void finished(){
		
	}
}