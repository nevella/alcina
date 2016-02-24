package cc.alcina.framework.common.client.cache;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.SortedMultiset;

public interface CacheCreators {
	public static interface CacheLongSetCreator extends Supplier<Set<Long>> {
	}

	public static interface CacheMultisetCreator<T> {
		public SortedMultiset<T, Set<Long>> get(CacheLookup cacheLookup,
				boolean concurrent);
	}

	public static interface CacheIdMapCreator
			extends Supplier<Map<Long, HasIdAndLocalId>> {
		public Map<Long, HasIdAndLocalId> get();
	}

	public static interface CachePrivateObjectCacheCreator
			extends Supplier<PrivateObjectCache> {
		public PrivateObjectCache get();
	}
}
