package cc.alcina.framework.entity.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;

public class CollectionCreatorsJvm {
	@RegistryLocation(registryPoint = ConcurrentMapCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	@ClientInstantiable
	public static class ConcurrentMapCreatorJvm extends ConcurrentMapCreator {
		@Override
		public <K, V> Map<K, V> createMap() {
			return new ConcurrentHashMap<>();
		}
	}
}
