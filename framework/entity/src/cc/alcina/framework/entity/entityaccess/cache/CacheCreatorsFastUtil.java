package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import cc.alcina.framework.common.client.cache.CacheCreators.CacheIdMapCreator;
import cc.alcina.framework.common.client.cache.CacheCreators.CacheLongSetCreator;
import cc.alcina.framework.common.client.cache.CacheCreators.CacheMultisetCreator;
import cc.alcina.framework.common.client.cache.CacheCreators.CachePrivateObjectCacheCreator;
import cc.alcina.framework.common.client.cache.CacheLookup;
import cc.alcina.framework.common.client.cache.CacheSizeProvider;
import cc.alcina.framework.common.client.cache.PrivateObjectCache;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.SortedMultiset;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class CacheCreatorsFastUtil {
	@RegistryLocation(registryPoint = CacheIdMapCreator.class, implementationType = ImplementationType.SINGLETON)
	public static class CacheIdMapCreatorJ2SE implements CacheIdMapCreator {
		@Override
		public Map<Long, HasIdAndLocalId> get() {
			return new ConcurrentSkipListMap<Long, HasIdAndLocalId>();
		}
	}

	@RegistryLocation(registryPoint = CacheLongSetCreator.class, implementationType = ImplementationType.SINGLETON)
	public static class CacheLongSetCreatorFastutil
			implements CacheLongSetCreator {
		@Override
		public Set<Long> get() {
			return new LongAVLTreeSet();
		}
	}

	@RegistryLocation(registryPoint = CacheMultisetCreator.class, implementationType = ImplementationType.SINGLETON)
	public static class CacheMultisetCreatorFastUtil<T>
			implements CacheMultisetCreator<T> {
		CacheLongSetCreator longSetCreator = Registry
				.impl(CacheLongSetCreator.class);

		@Override
		public SortedMultiset<T, Set<Long>> get(CacheLookup cacheLookup,
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
						CacheSizeProvider cacheSizeProvider = Registry
								.impl(CacheSizeProvider.class);
						int size = cacheSizeProvider
								.size(cacheLookup.toString());
						map = new Object2ObjectLinkedOpenHashMap<T, Set<Long>>();
						cacheSizeProvider.registerMap(cacheLookup.toString(),
								map);
					}
				};
			}
		}
	}

	@RegistryLocation(registryPoint = CachePrivateObjectCacheCreator.class, implementationType = ImplementationType.SINGLETON)
	public static class CachePrivateObjectCacheCreatorJ2SE
			implements CachePrivateObjectCacheCreator {
		@Override
		public PrivateObjectCache get() {
			return new DetachedEntityCache(
					() -> new Object2ObjectLinkedOpenHashMap(128),
					// not sorted - does it matter?
					() -> new Object2ObjectLinkedOpenHashMap(10000));
		}
	}
}
