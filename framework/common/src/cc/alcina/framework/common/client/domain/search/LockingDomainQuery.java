package cc.alcina.framework.common.client.domain.search;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.domain.search.DomainSearcher.MemoryStoreLocker;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;

@RegistryLocation(registryPoint = LockingDomainQuery.class, implementationType = ImplementationType.INSTANCE)
public class LockingDomainQuery extends DomainQuery<LockingDomainQuery> {
	public static final transient String CONTEXT_USE_SERIAL_STREAM = LockingDomainQuery.class
			.getName() + ".CONTEXT_USE_SERIAL_STREAM";

	protected SearchDefinition def;

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

	public void readLock(boolean lock) {
		Registry.impl(MemoryStoreLocker.class).readLock(lock);
	}

	protected void disposeStream() {
	}

	protected <T extends HasIdAndLocalId> Stream<T>
			getStream(Collection<T> values) {
		Stream<T> stream = values.stream().filter(v -> {
			for (DomainFilter filter : getFilters()) {
				if (!filter.asCollectionFilter().allow(v)) {
					return false;
				}
			}
			return true;
		});
		return stream;
	}
}