package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.LockingDomainQuery;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.util.CachingConcurrentMap;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils.SearchUtilsIdsHelper;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils.SearchUtilsIdsHelperSingleThreaded;

@RegistryLocation(registryPoint = LockingDomainQuery.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
public class LockingDomainQueryParallel<V extends HasIdAndLocalId>
		extends LockingDomainQuery<V> {
	private CachingConcurrentMap<Thread, DomainQueryThread> contexts = new CachingConcurrentMap<Thread, DomainQueryThread>(
			DomainQueryThread::new, 20);

	@Override
	protected void disposeStream() {
		contexts.getMap().values().forEach(DomainQueryThread::cleanup);
	}

	protected boolean filter(V v) {
		for (DomainFilter filter : getFilters()) {
			if (!filter.asCollectionFilter().allow(v)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected Stream<V> getStream(Collection<V> values) {
		boolean serial = LooseContext.is(CONTEXT_USE_SERIAL_STREAM);
		if (serial) {
			return values.stream().filter(this::filter);
		}
		LooseContextInstance snapshot = LooseContext.getContext().snapshot();
		return values.parallelStream().filter(v -> {
			contexts.get(Thread.currentThread()).snapshot(snapshot);
			return filter(v);
		});
	}

	@RegistryLocation(registryPoint = SearchUtilsIdsHelper.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class SearchUtilsIdsHelperMultiThreaded
			extends SearchUtilsIdsHelperSingleThreaded {
		@Override
		protected Map<String, Set<Long>> getMap() {
			return Collections.synchronizedMap(super.getMap());
		}
	}

	static class DomainQueryThread {
		private LooseContextInstance context;

		public DomainQueryThread(Thread thread) {
		}

		public void snapshot(LooseContextInstance snapshot) {
			if (this.context != null) {
				return;
			}
			this.context = LooseContext.getContext();
			context.push();
			context.putSnapshotProperties(snapshot);
		}

		void cleanup() {
			if (this.context != null) {
				context.pop();
			}
		}
	}
}
