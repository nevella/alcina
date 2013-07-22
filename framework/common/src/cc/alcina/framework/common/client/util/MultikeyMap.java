package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface MultikeyMap<V> extends Map {
	public abstract List<V> allValues();
	
	public abstract boolean containsKey(Object...objects);

	public abstract Map asMap(Object... objects);

	public abstract MultikeyMap createMap(int childDepth);

	public abstract V get(Object... objects);

	public abstract int getDepth();

	public abstract <T> Collection<T> items(Object... objects);

	public abstract void put(Object... objects);

	public abstract Object remove(Object... objects);

	public abstract <T> Collection<T> reverseItems(Object... objects);

	public abstract <T> Collection<T> reverseKeys(Object... objects);

	public abstract <T> Collection<T> reverseValues(Object... objects);

	public abstract MultikeyMap<V> swapKeysZeroAndOne();

	public abstract <T> Collection<T> values(Object... objects);

	void addValues(List<V> values);

	V getEnsure(boolean ensure, Object... objects);

	<T> Collection<T> keys(Object... objects);
}
