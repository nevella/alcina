package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

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
import cc.alcina.framework.common.client.util.SortedMultiset;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class DomainStoreCreatorsFastUtil {
	@RegistryLocation(registryPoint = DomainStoreIdMapCreator.class, implementationType = ImplementationType.SINGLETON)
	public static class DomainStoreIdMapCreatorJ2SE
			implements DomainStoreIdMapCreator {
		@Override
		public Map<Long, HasIdAndLocalId> get() {
			return new ConcurrentSkipListMap<Long, HasIdAndLocalId>();
		}
	}

	@RegistryLocation(registryPoint = DomainStoreLongSetCreator.class, implementationType = ImplementationType.SINGLETON)
	public static class DomainStoreLongSetCreatorFastutil
			implements DomainStoreLongSetCreator {
		@Override
		public Set<Long> get() {
			return new LongAVLTreeSet();
		}
	}

	@RegistryLocation(registryPoint = DomainStoreMultisetCreator.class, implementationType = ImplementationType.SINGLETON)
	public static class DomainStoreMultisetCreatorFastUtil<T>
			implements DomainStoreMultisetCreator<T> {
		DomainStoreLongSetCreator longSetCreator = Registry
				.impl(DomainStoreLongSetCreator.class);

		@Override
		public SortedMultiset<T, Set<Long>> get(DomainLookup cacheLookup,
				boolean concurrent) {
			if (concurrent) {
				return new ConcurrentSortedMultiset<>();
			} else {
				return new SortedMultiset<T, Set<Long>>() {
					@Override
					protected Set<Long> createSet() {
						return longSetCreator.get();
					}

					@Override
					protected void createTopMap() {
						map = new Object2ObjectLinkedOpenHashMap<T, Set<Long>>();
					}
				};
			}
		}
	}

	@RegistryLocation(registryPoint = DomainStorePrivateObjectCacheCreator.class, implementationType = ImplementationType.SINGLETON)
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
