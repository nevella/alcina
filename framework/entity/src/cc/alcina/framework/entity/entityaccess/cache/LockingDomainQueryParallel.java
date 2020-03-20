package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.LockingDomainQuery;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.util.CachingConcurrentMap;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils.SearchUtilsIdsHelper;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils.SearchUtilsIdsHelperSingleThreaded;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils.SearchUtilsRegExpHelper;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils.SearchUtilsRegExpHelperSingleThreaded;

@RegistryLocation(registryPoint = LockingDomainQuery.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
public class LockingDomainQueryParallel<V extends Entity>
		extends LockingDomainQuery<V> {
	private CachingConcurrentMap<Thread, DomainQueryThread> contexts = new CachingConcurrentMap<Thread, DomainQueryThread>(
			DomainQueryThread::new, 20);

	private Predicate<V> debugMatch;

	@Override
	protected void disposeStream() {
		contexts.getMap().values().forEach(DomainQueryThread::cleanup);
	}

	protected boolean filter(V v) {
		if (debugMatch != null && debugMatch.test(v)) {
			Ax.out(v);
			int debug = 3;
		}
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
		this.debugMatch = LooseContext.get(CONTEXT_DEBUG_MATCH);
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

	@RegistryLocation(registryPoint = SearchUtilsRegExpHelper.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class SearchUtilsRegExpHelperMultiThreaded
			extends SearchUtilsRegExpHelperSingleThreaded {
		@Override
		protected Map<String, RegExp> getMap() {
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
