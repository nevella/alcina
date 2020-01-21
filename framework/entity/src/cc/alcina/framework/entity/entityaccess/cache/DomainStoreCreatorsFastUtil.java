package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import cc.alcina.framework.common.client.domain.DomainCollections;
import cc.alcina.framework.common.client.domain.DomainLookup;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreIdMapCreator;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreLongSetCreator;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreMultisetCreator;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStorePrivateObjectCacheCreator;
import cc.alcina.framework.common.client.domain.PrivateObjectCache;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Multiset;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class DomainStoreCreatorsFastUtil {
	@RegistryLocation(registryPoint = DomainStoreIdMapCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class DomainStoreIdMapCreatorJ2SE
			implements DomainStoreIdMapCreator {
		@Override
		public Map<Long, HasIdAndLocalId> get() {
			return new ConcurrentSkipListMap<Long, HasIdAndLocalId>();
		}
	}

	@RegistryLocation(registryPoint = DomainStoreLongSetCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class DomainStoreLongSetCreatorFastutil
			implements DomainStoreLongSetCreator {
		@Override
		public Set<Long> get() {
			return new LongOpenHashSet();
		}
	}

	@RegistryLocation(registryPoint = DomainStoreMultisetCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class DomainStoreMultisetCreatorFastUtil<T>
			implements DomainStoreMultisetCreator<T> {
		DomainStoreLongSetCreator longSetCreator = Registry
				.impl(DomainStoreLongSetCreator.class);

		@Override
		public Multiset<T, Set<Long>> get(DomainLookup cacheLookup,
				boolean concurrent) {
			if (concurrent) {
				return new ConcurrentSortedMultiset<>();
			} else {
				return new Multiset<T, Set<Long>>() {
					@Override
					protected Set<Long> createSet() {
						return DomainCollections.get().createLightSet();
					}

					@Override
					protected void createTopMap() {
						map = new Object2ObjectLinkedOpenHashMap<T, Set<Long>>();
					}
				};
			}
		}
	}

	@RegistryLocation(registryPoint = DomainStorePrivateObjectCacheCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class DomainStorePrivateObjectCacheCreatorJ2SE
			implements DomainStorePrivateObjectCacheCreator {
		@Override
		public PrivateObjectCache get() {
			return new DetachedEntityCache(
					() -> new Object2ObjectLinkedOpenHashMap(128),
					// not sorted - does it matter?
					() -> new Object2ObjectLinkedOpenHashMap(10000));
		}
	}
}
