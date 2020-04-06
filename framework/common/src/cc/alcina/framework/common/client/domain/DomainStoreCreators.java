package cc.alcina.framework.common.client.domain;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Multiset;

public interface DomainStoreCreators {
	public static interface DomainStoreIdMapCreator
			extends Supplier<Map<Long, Entity>> {
		@Override
		public Map<Long, Entity> get();
	}

	public static interface DomainStoreLongSetCreator
			extends Supplier<Set<Long>> {
	}

	public static interface DomainStoreMultisetCreator<T> {
		public Multiset<T, Set<Long>> get(DomainLookup lookup);
	}
}
