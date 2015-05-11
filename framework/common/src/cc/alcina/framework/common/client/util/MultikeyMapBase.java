package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.ImmutableMap;

import com.google.gwt.user.client.rpc.GwtTransient;
import com.totsp.gwittir.client.beans.Converter;

public abstract class MultikeyMapBase<V> implements MultikeyMap<V>,
		Serializable {
	protected int depth;

	@GwtTransient
	protected Map delegate;

	protected transient ImmutableMap readonlyDelegate;

	@Override
	public void setDepth(int depth) {
		if (!delegate.keySet().isEmpty()) {
			throw new RuntimeException("cannot change depth once items added");
		}
		this.depth = depth;
	}

	public int size() {
		return this.delegate.size();
	}

	@Override
	public <T> Collection<T> values(Object... objects) {
		Map m = asMapEnsureDelegate(false, objects);
		return m == null ? null : m.values();
	}

	@Override
	public Map writeableDelegate() {
		return delegate;
	}

	@Override
	public boolean checkKeys(Object[] keys) {
		return true;
	}

	public MultikeyMap<V> swapKeysZeroAndOne() {
		MultikeyMap<V> swapped = createMap(getDepth());
		for (Object k0 : writeableDelegate().keySet()) {
			MultikeyMap<V> v = (MultikeyMap<V>) get(k0);
			for (Object k1 : v.writeableDelegate().keySet()) {
				swapped.put(k1, k0, v.get(k1));
			}
		}
		return swapped;
	}

	public void addValues(List<V> values) {
		if (getDepth() == 1) {
			values.addAll(writeableDelegate().values());
		} else {
			for (Object k : writeableDelegate().keySet()) {
				((MultikeyMap<V>) asMap(k)).addValues(values);
			}
		}
	}

	public List<V> allValues() {
		ArrayList<V> all = new ArrayList<V>();
		addValues(all);
		return all;
	}

	MultikeyMap asMap(boolean ensure, Object... objects) {
		MultikeyMap m = (MultikeyMap) getWithKeys(ensure, 0, objects);
		return m;
	}

	public V getEnsure(boolean ensure, Object... objects) {
		assert objects.length == getDepth();
		return (V) getWithKeys(ensure, 0, objects);
	}

	Object getWithKeys(boolean ensure, int ignoreCount, Object... objects) {
		MultikeyMap m = this;
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
				if (ensure && i != getDepth() - 1) {
					// only use ensure if we're ensuring a map, not a key
					o = createMap(getDepth() - i - 1);
					m.writeableDelegate().put(k, o);
					m = (MultikeyMap) o;
				} else {
					return null;
				}
			}
		}
		return m;
	}

	public void put(Object... objects) {
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

	@Override
	public V ensure(Supplier<V> supplier, Object... objects) {
		V v = get(objects);
		if (v == null) {
			Object[] arr = new Object[objects.length + 1];
			System.arraycopy(objects, 0, arr, 0, objects.length);
			v = supplier.get();
			arr[objects.length] = v;
			put(arr);
		}
		return v;
	}

	public V remove(Object... objects) {
		return remove(false, objects);
	}

	private V remove(boolean allowNonValue, Object... objects) {
		int trim = objects.length == getDepth() + 1 ? 1 : 0;
		assert objects.length == getDepth() + trim || allowNonValue;
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
		if (getDepth() != other.getDepth()) {
			throw new RuntimeException("Incompatible depth");
		}
		if (getDepth() == 1) {
			writeableDelegate().putAll(other.writeableDelegate());
		} else {
			for (Object key : other.writeableDelegate().keySet()) {
				((MultikeyMap<V>) asMap(key)).putMulti((MultikeyMap<V>) other
						.asMap(key));
			}
		}
	}

	public List<List> asTuples(int maxDepth) {
		List<List> result = new ArrayList<List>();
		result.add(new ArrayList<Object>());// empty key, depth 0
		for (int depth = 0; depth < maxDepth; depth++) {
			List<List> next = new ArrayList<List>();
			for (List key : result) {
				Object[] kArr = (Object[]) key.toArray(new Object[key.size()]);
				for (Object k2 : keys(kArr)) {
					List nextK = new ArrayList(key);
					nextK.add(k2);
					if (depth == getDepth() - 1) {
						Object[] kArr2 = (Object[]) nextK
								.toArray(new Object[nextK.size()]);
						nextK.add(get(kArr2));
					}
					next.add(nextK);
				}
			}
			result = next;
		}
		return result;
	}

	@Override
	public void addTuples(List<List> tuples) {
		for (List list : tuples) {
			put(list.toArray());
		}
	}

	public <T> void addTupleObjects(List<T> tupleObjects,
			Converter<T, List> converter) {
		for (T t : tupleObjects) {
			put(converter.convert(t).toArray());
		}
	}

	public <T> List<T> asTupleObjects(int maxDepth, Converter<List, T> converter) {
		List<List> tuples = asTuples(maxDepth);
		return CollectionFilters.convert(tuples, converter);
	}

	@Override
	public MultikeyMap asMap(Object... objects) {
		return asMap(true, objects);
	}

	@Override
	public MultikeyMap asMapEnsure(boolean ensure, Object... objects) {
		return asMap(ensure, objects);
	}

	@Override
	public Map delegate() {
		if (readonlyDelegate == null) {
			readonlyDelegate = new ImmutableMap(delegate);
		}
		return readonlyDelegate;
	}

	@Override
	public V get(Object... objects) {
		return (V) getEnsure(true, objects);
	}

	@Override
	public int getDepth() {
		return this.depth;
	}

	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@Override
	public <T> Collection<T> items(Object... objects) {
		if (objects.length >= depth) {
			throw new IllegalArgumentException(
					"items() must have fewer than <depth> keys");
		}
		return keys(objects);
	}

	protected Map asMapEnsureDelegate(boolean ensure, Object... objects) {
		MultikeyMapBase mkm = (MultikeyMapBase) asMap(ensure, objects);
		return mkm == null ? null : mkm.delegate;
	}

	@Override
	public <T> Collection<T> keys(Object... objects) {
		Map m = asMapEnsureDelegate(false, objects);
		return m == null ? null : m.keySet();
	}

	public Set keySet() {
		return this.delegate.keySet();
	}

	@Override
	public <T> Collection<T> reverseItems(Object... objects) {
		return (Collection) (objects.length == depth ? reverseValues(objects)
				: reverseKeys(objects));
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public void stripNonDuplicates(int depth) {
		List<List> keyTuples = asTuples(depth);
		for (List list : keyTuples) {
			MultikeyMap forTuple = asMap(list.toArray());
			if (forTuple.size() == 1) {
				remove(true, list.toArray());
			}
		}
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("mkm - depth %s - tuples: \n%s", depth,
				CommonUtils.join(asTuples(depth), "\n"));
	}
}
