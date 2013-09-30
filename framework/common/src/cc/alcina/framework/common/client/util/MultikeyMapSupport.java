package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

// basically a mixin. Now that multikeymap doesn't extend map, not necessary...but works ok
public class MultikeyMapSupport<V> implements Serializable {
	private MultikeyMap<V> multi;

	public MultikeyMapSupport() {
	}

	public MultikeyMapSupport(MultikeyMap<V> multi) {
		this.multi = multi;
	}

	public MultikeyMap<V> swapKeysZeroAndOne() {
		MultikeyMap<V> swapped = multi.createMap(multi.getDepth());
		for (Object k0 : multi.writeableDelegate().keySet()) {
			MultikeyMap<V> v = (MultikeyMap<V>) multi.get(k0);
			for (Object k1 : v.writeableDelegate().keySet()) {
				swapped.put(k1, k0, v.get(k1));
			}
		}
		return swapped;
	}

	void addValues(List<V> values) {
		if (multi.getDepth() == 1) {
			values.addAll(multi.writeableDelegate().values());
		} else {
			for (Object k : multi.writeableDelegate().keySet()) {
				((MultikeyMap<V>) multi.asMap(k)).addValues(values);
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
		assert objects.length == multi.getDepth();
		return (V) getWithKeys(ensure, 0, objects);
	}

	Object getWithKeys(boolean ensure, int ignoreCount, Object... objects) {
		MultikeyMap m = multi;
		int last = objects.length - 1 - ignoreCount;
		for (int i = 0; i <= last; i++) {
			Object k = objects[i];
			Object o = m.writeableDelegate().get(k);
			if (o != null) {
				if (i == last) {
					return o;
				} else {
					m = (MultikeyMap) o;
				}
			} else {
				if (ensure && i != multi.getDepth() - 1) {
					// only use ensure if we're ensuring a map, not a key
					o = multi.createMap(multi.getDepth() - i - 1);
					m.writeableDelegate().put(k, o);
					m = (MultikeyMap) o;
				} else {
					return null;
				}
			}
		}
		return m;
	}

	void put(Object... objects) {
		Map m = getMapForObjects(true, 2, objects);
		Object key = objects[objects.length - 2];
		if (m instanceof SortedMap && key == null) {
			RuntimeException ex = new RuntimeException(
					"Invalid keys for sorted multikey put - "
							+ Arrays.asList(objects));
			throw ex;
		}
		m.put(key, objects[objects.length - 1]);
	}

	private Map getMapForObjects(boolean ensure, int length, Object... objects) {
		Object withKeys = getWithKeys(ensure, length, objects);
		MultikeyMap mkm = (MultikeyMap) withKeys;
		return mkm != null ? mkm.writeableDelegate() : null;
	}

	public V remove(Object... objects) {
		int trim = objects.length == multi.getDepth() + 1 ? 1 : 0;
		assert objects.length == multi.getDepth() + trim;
		// ignore last value (k/k/k/v) if it's there
		Map m = getMapForObjects(false, 1 + trim, objects);
		if (m == null) {
			return null;
		}
		V result = (V) m.remove(objects[objects.length - 1 - trim]);
		for (int keyIndex = objects.length - 2 - trim; keyIndex >= 0; keyIndex--) {
			Map parent = getMapForObjects(false, objects.length - keyIndex,
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
		Map m = getMapForObjects(false, 1, objects);
		return m != null && m.containsKey(objects[objects.length - 1]);
	}

	public void putMulti(MultikeyMap<V> other) {
		if (multi.getDepth() != other.getDepth()) {
			throw new RuntimeException("Incompatible depth");
		}
		if (multi.getDepth() == 1) {
			multi.writeableDelegate().putAll(other.writeableDelegate());
		} else {
			for (Object key : other.writeableDelegate().keySet()) {
				((MultikeyMap<V>) multi.asMap(key))
						.putMulti((MultikeyMap<V>) other.asMap(key));
			}
		}
	}

	public List<List> asTuples() {
		List<List> result = new ArrayList<List>();
		result.add(new ArrayList<Object>());// empty key, depth 0
		for (int depth = 0; depth < multi.getDepth(); depth++) {
			List<List> next = new ArrayList<List>();
			for (List key : result) {
				Object[] kArr = (Object[]) key.toArray(new Object[key.size()]);
				for (Object k2 : multi.keys(kArr)) {
					List nextK = new ArrayList(key);
					nextK.add(k2);
					if(depth == multi.getDepth()-1){
						Object[] kArr2 = (Object[]) nextK.toArray(new Object[nextK.size()]);
						nextK.add(multi.get(kArr2));
					}
					next.add(nextK);
					
				}
			}
			result = next;
		}
		return result;
	}
}