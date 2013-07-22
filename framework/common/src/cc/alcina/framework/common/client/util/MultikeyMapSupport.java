package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// basically a mixin
public class MultikeyMapSupport<V> implements Serializable {
	private MultikeyMap<V> map;

	public MultikeyMapSupport(MultikeyMap<V> map) {
		this.map = map;
	}

	public MultikeyMap<V> swapKeysZeroAndOne() {
		MultikeyMap<V> swapped = map.createMap(map.getDepth());
		for (Object k0 : map.keySet()) {
			MultikeyMap<V> v = (MultikeyMap<V>) map.get(k0);
			for (Object k1 : v.keySet()) {
				swapped.put(k1, k0, v.get(k1));
			}
		}
		return swapped;
	}

	void addValues(List<V> values) {
		if (map.getDepth() == 1) {
			values.addAll(map.values());
		} else {
			for (Object k : map.keySet()) {
				((MultikeyMap<V>) map.get(k)).addValues(values);
			}
		}
	}

	List<V> allValues() {
		ArrayList<V> all = new ArrayList<V>();
		addValues(all);
		return all;
	}

	MultikeyMap asMap(boolean ensure, Object... objects) {
		MultikeyMap m = (MultikeyMap) getWithKeys(ensure, 0, objects);
		return m;
	}

	V getEnsure(boolean ensure, Object... objects) {
		assert objects.length == map.getDepth();
		return (V) getWithKeys(ensure, 0, objects);
	}

	Object getWithKeys(boolean ensure, int ignoreCount, Object... objects) {
		Map m = map;
		int last = objects.length - 1 - ignoreCount;
		for (int i = 0; i <= last; i++) {
			Object k = objects[i];
			Object o = m.get(k);
			if (o != null) {
				if (i == last) {
					return o;
				} else {
					m = (Map) o;
				}
			} else {
				if (ensure && i != map.getDepth() - 1) {
					// only use ensure if we're ensuring a map, not a key
					o = map.createMap(map.getDepth() - i - 1);
					m.put(k, o);
					m = (Map) o;
				} else {
					return null;
				}
			}
		}
		return m;
	}

	void put(Object... objects) {
		Map m = (Map) getWithKeys(true, 2, objects);
		m.put(objects[objects.length - 2], objects[objects.length - 1]);
	}

	public V remove(Object... objects) {
		int trim = objects.length == map.getDepth() + 1 ? 1 : 0;
		assert objects.length == map.getDepth() + trim;
		// ignore last value (k/k/k/v) if it's there
		Map m = (Map) getWithKeys(false, 1 + trim, objects);
		if (m == null) {
			return null;
		}
		V result = (V) m.remove(objects[objects.length - 1 - trim]);
		for (int keyIndex = objects.length - 2 - trim; keyIndex >= 0; keyIndex--) {
			Map parent = (Map) getWithKeys(false, objects.length - keyIndex,
					objects);
			if (m.isEmpty()) {
				Object keyWithEmptyMap = objects[keyIndex];
				parent.remove(keyWithEmptyMap);
				m = parent;
			} else {
				break;
			}
		}
		return result;
	}

	public boolean containsKey(Object... objects) {
		Map m = (Map) getWithKeys(false, 1, objects);
		return m != null && m.containsKey(objects[objects.length - 1]);
	}
}