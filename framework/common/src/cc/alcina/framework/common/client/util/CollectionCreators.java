package cc.alcina.framework.common.client.util;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.domain.Entity;

public class CollectionCreators {
	public interface DelegateMapCreator {
		Map createDelegateMap(int depthFromRoot, int depth);

		default boolean isSorted(Map m) {
			return m instanceof SortedMap;
		}
	}

	public static interface IdMapCreator extends Supplier<Map<Long, Entity>> {
		@Override
		public Map<Long, Entity> get();
	}

	public static interface LongSetCreator extends Supplier<Set<Long>> {
	}

	public static interface MultisetCreator<K, V> {
		public Multiset<K, Set<V>> create(Class<K> keyClass,
				Class<V> valueClass);
	}

	public interface MapCreator<K, V> extends Supplier<Map<K, V>> {
	}
}
