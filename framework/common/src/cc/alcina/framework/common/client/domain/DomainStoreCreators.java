package cc.alcina.framework.common.client.domain;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Multiset;

public interface DomainStoreCreators {
	public static interface DomainStoreIdMapCreator
			extends Supplier<Map<Long, HasIdAndLocalId>> {
		@Override
		public Map<Long, HasIdAndLocalId> get();
	}

	public static interface DomainStoreLongSetCreator
			extends Supplier<Set<Long>> {
	}

	public static interface DomainStoreMultisetCreator<T> {
		public Multiset<T, Set<Long>> get(DomainLookup lookup);
	}

	public static interface DomainStorePrivateObjectCacheCreator
			extends Supplier<PrivateObjectCache> {
		@Override
		public PrivateObjectCache get();
	}
}
