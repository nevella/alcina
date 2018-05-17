package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.cache.search.MemoryStoreQuery;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.util.CachingConcurrentMap;

@RegistryLocation(registryPoint = MemoryStoreQuery.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
public class MemoryStoreQueryParallel extends MemoryStoreQuery {
	@Override
	protected <T extends HasIdAndLocalId> Stream<T>
			getStream(Collection<T> values) {
		CachingConcurrentMap<Thread, MemoryStoreQueryThread> contexts = new CachingConcurrentMap<Thread, MemoryStoreQueryThread>(
				MemoryStoreQueryThread::new, 20);
		LooseContextInstance snapshot = LooseContext.getContext().snapshot();
		try {
			Stream<T> stream = null;
			if (LooseContext.is(CONTEXT_USE_SERIAL_STREAM)) {
				stream = values.stream();
			} else {
				stream = values.parallelStream();
			}
			stream = stream.filter(v -> {
				contexts.get(Thread.currentThread()).snapshot(snapshot);
				for (CacheFilter filter : getFilters()) {
					if (!filter.asCollectionFilter().allow(v)) {
						return false;
					}
				}
				return true;
			});
			return stream;
		} finally {
			contexts.getMap().values().forEach(MemoryStoreQueryThread::cleanup);
		}
	}

	static class MemoryStoreQueryThread {
		public MemoryStoreQueryThread(Thread thread) {
		}

		public void snapshot(LooseContextInstance snapshot) {
			if (snapshot != null) {
				return;
			}
			LooseContext.push();
			LooseContext.putContext(snapshot);
		}

		void cleanup() {
			LooseContext.pop();
		}
	}
}
