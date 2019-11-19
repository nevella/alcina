package cc.alcina.framework.common.client.domain.search;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.domain.search.DomainSearcher.DomainLocker;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;

@RegistryLocation(registryPoint = LockingDomainQuery.class, implementationType = ImplementationType.INSTANCE)
public class LockingDomainQuery<V extends HasIdAndLocalId>
		extends DomainQuery<V> {
	public static final transient String CONTEXT_USE_SERIAL_STREAM = LockingDomainQuery.class
			.getName() + ".CONTEXT_USE_SERIAL_STREAM";

	public static final transient String CONTEXT_DEBUG_MATCH = LockingDomainQuery.class
			.getName() + ".CONTEXT_DEBUG_MATCH";

	protected SearchDefinition def;

	public LockingDomainQuery() {
		super(null);
	}

	@Override
	public List<V> list() {
		Collection<V> values = Registry.impl(SearcherCollectionSource.class)
				.getCollectionFor(clazz, def);
		try {
			Stream<V> stream = getStream(values);
			return stream.collect(Registry.impl(ListCollector.class).toList());
		} finally {
			disposeStream();
		}
	}

	public void readLock(boolean lock) {
		Registry.impl(DomainLocker.class).readLock(lock);
	}

	public void setQueryClass(Class<V> clazz) {
		this.clazz = clazz;
	}

	protected void disposeStream() {
	}

	protected Stream<V> getStream(Collection<V> values) {
		Stream<V> stream = values.stream().filter(v -> {
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