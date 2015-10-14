package cc.alcina.framework.entity.entityaccess.cache;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import cc.alcina.framework.common.client.cache.CacheCreators.CacheIdMapCreator;
import cc.alcina.framework.common.client.cache.CacheCreators.CacheLongSetCreator;
import cc.alcina.framework.common.client.cache.CacheCreators.CacheMultisetCreator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.SortedMultiset;

public class CacheCreatorsFastUtil {
	@RegistryLocation(registryPoint = CacheLongSetCreator.class)
	public static class CacheLongSetCreatorFastutil implements
			CacheLongSetCreator {
		@Override
		public Set<Long> get() {
			return new LongAVLTreeSet();
		}
	}

	@RegistryLocation(registryPoint = CacheMultisetCreator.class)
	public static class CacheMultisetCreatorFastUtil<T> implements
			CacheMultisetCreator<T> {
		CacheLongSetCreator longSetCreator = Registry
				.impl(CacheLongSetCreator.class);

		@Override
		public SortedMultiset<T, Set<Long>> get(boolean concurrent) {
			if (concurrent) {
				return new ConcurrentSortedMultiset<>();
			} else {
				
				return new SortedMultiset<T, Set<Long>>() {
					@Override
					protected Set<Long> createSet() {
						return longSetCreator.get();
					}

					@Override
					protected Map<T, Set<Long>> createTopMap() {
						return new Object2ObjectLinkedOpenHashMap<T, Set<Long>>();
					}
				};
			}
		}
	}

	@RegistryLocation(registryPoint = CacheIdMapCreator.class)
	public static class CacheIdMapCreatorJ2SE implements CacheIdMapCreator {
		@Override
		public Map<Long, HasIdAndLocalId> get() {
			return new ConcurrentSkipListMap<Long, HasIdAndLocalId>();
		}
	}
}
