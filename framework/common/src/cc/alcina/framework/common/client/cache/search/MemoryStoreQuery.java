package cc.alcina.framework.common.client.cache.search;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.cache.CacheQuery;
import cc.alcina.framework.common.client.cache.search.MemcacheSearcher.MemoryStoreLocker;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;

@RegistryLocation(registryPoint = MemoryStoreQuery.class, implementationType = ImplementationType.INSTANCE)
public class MemoryStoreQuery extends CacheQuery<MemoryStoreQuery> {
	protected SearchDefinition def;

	public static final transient String CONTEXT_USE_SERIAL_STREAM = MemoryStoreQuery.class
			.getName() + ".CONTEXT_USE_SERIAL_STREAM";

	@Override
	public <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
		Collection<T> values = Registry.impl(SearcherCollectionSource.class)
				.getCollectionFor(clazz, def);
		try {
			Stream<T> stream = getStream(values);
			return stream.collect(Registry.impl(ListCollector.class).toList());
		} finally {
			disposeStream();
		}
	}

	protected void disposeStream() {
	}

	public void readLock(boolean lock) {
		Registry.impl(MemoryStoreLocker.class).readLock(lock);
	}

	protected <T extends HasIdAndLocalId> Stream<T>
			getStream(Collection<T> values) {
		Stream<T> stream = values.stream().filter(v -> {
			for (CacheFilter filter : getFilters()) {
				if (!filter.asCollectionFilter().allow(v)) {
					return false;
				}
			}
			return true;
		});
		return stream;
	}
}