package cc.alcina.framework.entity.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CollectionCreators.HashMapCreator;
<<<<<<< HEAD
import cc.alcina.framework.common.client.util.CollectionCreators.UnsortedMapCreator;
=======
>>>>>>> main
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class CollectionCreatorsJvm {
	@RegistryLocation(registryPoint = ConcurrentMapCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	@ClientInstantiable
	public static class ConcurrentMapCreatorJvm extends ConcurrentMapCreator {
		@Override
		public <K, V> Map<K, V> createMap() {
			return new ConcurrentHashMap<>();
		}
	}

	@RegistryLocation(registryPoint = CollectionCreators.HashMapCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	@ClientInstantiable
	public static class HashMapCreatorJvm extends HashMapCreator {
		@Override
		public <K, V> Map<K, V> copy(Map<K, V> toClone) {
			if (toClone instanceof Object2ObjectLinkedOpenHashMap) {
				return ((Object2ObjectLinkedOpenHashMap) toClone).clone();
			} else {
				return new Object2ObjectLinkedOpenHashMap<>(toClone);
			}
		}

		@Override
		public <K, V> Map<K, V> create() {
			return new Object2ObjectLinkedOpenHashMap<>();
		}
	}
<<<<<<< HEAD

	@RegistryLocation(registryPoint = UnsortedMapCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class UnsortedMapCreatorJvm extends UnsortedMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new Object2ObjectLinkedOpenHashMap<>();
		}
	}
=======
>>>>>>> main
}
