package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gwt.user.client.rpc.GwtTransient;
import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.collections.ImmutableMap;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;

public abstract class MultikeyMapBase<V>
		implements MultikeyMap<V>, Serializable {
	protected int depth;

	@GwtTransient
	protected Map delegate;

	protected transient ImmutableMap readonlyDelegate;

	protected int depthFromRoot;

	@GwtTransient
	protected DelegateMapCreator delegateMapCreator;

	public MultikeyMapBase(int depth, int depthFromRoot) {
		this(depth, depthFromRoot, null);
	}

	public MultikeyMapBase(int depth, int depthFromRoot,
			DelegateMapCreator delegateMapCreator) {
		this.depth = depth;
		this.depthFromRoot = depthFromRoot;
		this.delegateMapCreator = delegateMapCreator;
		ensureDelegateMapCreator();
		this.delegate = createDelegateMap();
	}

	@Override
	public <T> void addTupleObjects(List<T> tupleObjects,
			Converter<T, List> converter) {
		for (T t : tupleObjects) {
			put(converter.convert(t).toArray());
		}
	}

	@Override
	public void addTuples(List<List> tuples) {
		for (List list : tuples) {
			put(list.toArray());
		}
	}

	@Override
	public void addValues(List<V> values) {
		if (getDepth() == 1) {
			values.addAll(writeableDelegate().values());
		} else {
			for (Object k : writeableDelegate().keySet()) {
				((MultikeyMap<V>) asMap(k)).addValues(values);
			}
		}
	}

	@Override
	public List<V> allValues() {
		ArrayList<V> all = new ArrayList<V>();
		addValues(all);
		return all;
	}

	MultikeyMap<V> asMap(boolean ensure, Object... objects) {
		MultikeyMap m = (MultikeyMap) getWithKeys(ensure, 0, objects);
		return m;
	}

	@Override
	public MultikeyMap<V> asMap(Object... objects) {
		return asMap(true, objects);
	}

	@Override
	public MultikeyMap<V> asMapEnsure(boolean ensure, Object... objects) {
		return asMap(ensure, objects);
	}

	protected Map asMapEnsureDelegate(boolean ensure, Object... objects) {
		MultikeyMapBase mkm = (MultikeyMapBase) asMap(ensure, objects);
		return mkm == null ? null : mkm.delegate;
	}

	@Override
	public <T> List<T> asTupleObjects(int maxDepth,
			Converter<List, T> converter) {
		List<List> tuples = asTuples(maxDepth);
		return tuples.stream().map(converter).collect(Collectors.toList());
	}

	@Override
	public List<List> asTuples(int maxDepth) {
		List<List> result = new ArrayList<List>();
		result.add(new ArrayList<Object>());// empty key, depth 0
		for (int depth = 0; depth < maxDepth; depth++) {
			List<List> next = new ArrayList<List>();
			for (List key : result) {
				Object[] kArr = (Object[]) key.toArray(new Object[key.size()]);
				Collection<Object> keys = keys(kArr);
				if (keys == null) {
					// throw new RuntimeException("mis-put, methinks");
					keys = Arrays.asList(new MissingObject());
				}
				for (Object k2 : keys) {
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
	public boolean checkKeys(Object[] keys) {
		return true;
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public boolean containsKey(Object... objects) {
		Map m = getMapForObjects(false, 1, objects);
		return m != null && m.containsKey(objects[objects.length - 1]);
	}

	protected Map createDelegateMap() {
		return delegateMapCreator.createDelegateMap(depthFromRoot, depth);
	}

	@Override
	public Map delegate() {
		if (readonlyDelegate == null) {
			readonlyDelegate = new ImmutableMap(delegate);
		}
		return readonlyDelegate;
	}

	@Override
	public V ensure(Supplier<V> supplier, Object... objects) {
		V v = get(objects);
		if (v == null && !containsKey(objects)) {
			Object[] arr = new Object[objects.length + 1];
			System.arraycopy(objects, 0, arr, 0, objects.length);
			v = supplier.get();
			arr[objects.length] = v;
			put(arr);
		}
		return v;
	}

	protected abstract DelegateMapCreator ensureDelegateMapCreator();

	@Override
	public V get(Object... objects) {
		return (V) getEnsure(false, objects);
	}

	@Override
	public int getDepth() {
		return this.depth;
	}

	@Override
	public V getEnsure(boolean ensure, Object... objects) {
		assert objects.length == getDepth();
		return (V) getWithKeys(ensure, 0, objects);
	}

	private Map getMapForObjects(boolean ensure, int length,
			Object... objects) {
		Object withKeys = getWithKeys(ensure, length, objects);
		MultikeyMap mkm = (MultikeyMap) withKeys;
		return mkm != null ? mkm.writeableDelegate() : null;
	}

	Object getWithKeys(boolean ensure, int ignoreCount, Object... objects) {
		MultikeyMap map = this;
		int last = objects.length - 1 - ignoreCount;
		for (int idx = 0; idx <= last; idx++) {
			Object key = objects[idx];
			if (key == null && this instanceof SortedMultikeyMap) {
				// invalid key, would throw NPE on a treemap
				return null;
			}
			Object object = map.writeableDelegate().get(key);
			if (object != null) {
				if (idx == last) {
					return object;
				} else {
					map = (MultikeyMap) object;
				}
			} else {
				if (ensure && idx != getDepth() - 1) {
					// only use ensure if we're ensuring a map, not a key
					object = createMap(getDepth() - idx - 1);
					map.writeableDelegate().put(key, object);
					map = (MultikeyMap) object;
				} else {
					return null;
				}
			}
		}
		return map;
	}

	@Override
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

	@Override
	public <T> Collection<T> keys(Object... objects) {
		Map m = asMapEnsureDelegate(false, objects);
		return m == null ? null : m.keySet();
	}

	@Override
	public <T> Set keySet() {
		return this.delegate.keySet();
	}

	@Override
	public Object put(Object... objects) {
		Map m = getMapForObjects(true, 2, objects);
		Object key = objects[objects.length - 2];
		Object existing = m.get(key);
		m.put(key, objects[objects.length - 1]);
		return existing;
	}

	@Override
	public void putMulti(MultikeyMap<V> other) {
		if (getDepth() != other.getDepth()) {
			throw new RuntimeException("Incompatible depth");
		}
		if (getDepth() == 1) {
			writeableDelegate().putAll(other.writeableDelegate());
		} else {
			for (Object key : other.writeableDelegate().keySet()) {
				((MultikeyMap<V>) asMap(key))
						.putMulti((MultikeyMap<V>) other.asMap(key));
			}
		}
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
		for (int keyIndex = objects.length - 2
				- trim; keyIndex >= 0; keyIndex--) {
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

	@Override
	public V remove(Object... objects) {
		return remove(false, objects);
	}

	@Override
	public <T> Collection<T> reverseItems(Object... objects) {
		return (Collection) (objects.length == depth ? reverseValues(objects)
				: reverseKeys(objects));
	}

	@Override
	public void setDepth(int depth) {
		if (!delegate.keySet().isEmpty()) {
			throw new RuntimeException("cannot change depth once items added");
		}
		this.depth = depth;
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public void sortKeys(Object... objects) {
		Map delegate = asMapEnsureDelegate(true, objects);
		Map copyHolder = new HashMap<>();
		copyHolder.putAll(delegate);
		delegate.clear();
		Object nullKeyValue = null;
		if (!(delegate instanceof SortedMap) && copyHolder.containsKey(null)) {
			delegate.put(null, copyHolder.remove(null));
		}
		copyHolder.keySet().stream().sorted()
				.forEach(key -> delegate.put(key, copyHolder.get(key)));
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

	@Override
	public String toString() {
		return Ax.format("mkm - depth %s - tuples: \n%s", depth,
				CommonUtils.join(asTuples(depth), "\n"));
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

	static class MissingObject {
		@Override
		public String toString() {
			return "Missing object";
		};
	}
}
