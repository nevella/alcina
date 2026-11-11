package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.AlcinaCollections;

public interface IntLookup<V> {
	V get(int key);

	void put(int key, V value);

	boolean remove(int key);

	List<V> values();

	public static class MapAdapter<V> implements IntLookup<V> {
		Map<Integer, V> map = AlcinaCollections.newCoarseIntHashMap();

		@Override
		public V get(int key) {
			return map.get(key);
		}

		@Override
		public boolean remove(int key) {
			return map.remove(key) != null;
		}

		@Override
		public void put(int key, V value) {
			map.put(key, value);
		}

		@Override
		public List<V> values() {
			return map.values().stream().collect(Collectors.toList());
		}
	}
}
