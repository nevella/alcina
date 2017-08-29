package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.cache.search.MemcacheSearcher.TQuery;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.util.CachingConcurrentMap;

@RegistryLocation(registryPoint = TQuery.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
public class TQueryParallel extends TQuery {
	static class TQueryThread {
		public TQueryThread(Thread thread) {
		}

		void cleanup() {
			LooseContext.pop();
		}

		public void snapshot(LooseContextInstance snapshot) {
			if (snapshot != null) {
				return;
			}
			LooseContext.push();
			LooseContext.putContext(snapshot);
		}
	}

	@Override
	protected <T extends HasIdAndLocalId> Stream<T>
			getStream(Collection<T> values) {
		CachingConcurrentMap<Thread, TQueryThread> contexts = new CachingConcurrentMap<Thread, TQueryThread>(
				TQueryThread::new, 20);
		LooseContextInstance snapshot = LooseContext.getContext().snapshot();
		try {
			Stream<T> stream = values.parallelStream().filter(v -> {
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
			contexts.getMap().values().forEach(TQueryThread::cleanup);
		}
	}
}
