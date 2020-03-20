package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import cc.alcina.framework.common.client.domain.DomainLookup;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreIdMapCreator;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreLongSetCreator;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreMultisetCreator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.TransactionalLongSet;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.TransactionalMap;

public class DomainStoreCreatorsFastUtil {
	@RegistryLocation(registryPoint = DomainStoreIdMapCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class DomainStoreIdMapCreatorJ2SE
			implements DomainStoreIdMapCreator {
		@Override
		public Map<Long, Entity> get() {
			return new ConcurrentSkipListMap<Long, Entity>();
		}
	}

	@RegistryLocation(registryPoint = DomainStoreLongSetCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class DomainStoreLongSetCreatorFastutil
			implements DomainStoreLongSetCreator {
		@Override
		public Set<Long> get() {
			return new TransactionalLongSet();
		}
	}

	@RegistryLocation(registryPoint = DomainStoreMultisetCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class DomainStoreMultisetCreatorFastUtil<T>
			implements DomainStoreMultisetCreator<T> {
		DomainStoreLongSetCreator longSetCreator = Registry
				.impl(DomainStoreLongSetCreator.class);

		@Override
		public Multiset<T, Set<Long>> get(DomainLookup cacheLookup) {
			return new Multiset<T, Set<Long>>() {
				@Override
				protected Set<Long> createSet() {
					return longSetCreator.get();
				}

				@Override
				protected void createTopMap() {
					map = new TransactionalMap(Object.class, Set.class);
				}
			};
		}
	}
}
