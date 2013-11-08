package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MultikeyMap<V> {
	public abstract List<V> allValues();

	public abstract MultikeyMap asMap(Object... objects);

	public abstract boolean containsKey(Object... objects);

	public abstract MultikeyMap createMap(int childDepth);

	public abstract Map delegate();

	public abstract V get(Object... objects);

	public abstract int getDepth();

	public boolean isEmpty();

	public abstract <T> Collection<T> items(Object... objects);

	public Set keySet();

	public abstract void put(Object... objects);

	public abstract Object remove(Object... objects);

	public abstract <T> Collection<T> reverseItems(Object... objects);

	public abstract <T> Collection<T> reverseKeys(Object... objects);

	public abstract <T> Collection<T> reverseValues(Object... objects);

	public int size();

	public abstract MultikeyMap<V> swapKeysZeroAndOne();

	public abstract <T> Collection<T> values(Object... objects);

	void addValues(List<V> values);

	V getEnsure(boolean ensure, Object... objects);

	<T> Collection<T> keys(Object... objects);

	void putMulti(MultikeyMap<V> multi);

	void setDepth(int depth);

	public abstract Map writeableDelegate();

	public abstract void clear();

	public abstract List<List> asTuples(int depth);

	public abstract boolean checkKeys(Object[] keys);
}
