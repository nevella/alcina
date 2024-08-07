package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.totsp.gwittir.client.beans.Converter;

public interface MultikeyMap<V> {
	default void addInteger(int delta, Object... objects) {
		Integer value = (Integer) ensure(() -> (V) (Object) Integer.valueOf(0),
				objects);
		value += delta;
		List<Object> list = new ArrayList<>(Arrays.asList(objects));
		list.add(value);
		put(list.toArray());
	}

	public abstract <T> void addTupleObjects(List<T> tupleObjects,
			Converter<T, List> converter);

	public abstract void addTuples(List<List> tuples);

	void addValues(List<V> values);

	public abstract List<V> allValues();

	public abstract MultikeyMap asMap(Object... objects);

	public abstract MultikeyMap<V> asMapEnsure(boolean ensure,
			Object... objects);

	public abstract <T> List<T> asTupleObjects(int maxDepth,
			Converter<List, T> converter);

	public abstract List<List> asTuples(int depth);

	/*
	 * Checks if the MKM allows the keys (basically for null checks and sorted
	 * maps)
	 */
	public abstract boolean checkKeys(Object[] keys);

	public abstract void clear();

	public abstract boolean containsKey(Object... objects);

	public abstract MultikeyMap createMap(int childDepth);

	public abstract Map delegate();

	public abstract V ensure(Supplier<V> supplier, Object... objects);

	public abstract V get(Object... objects);

	public abstract int getDepth();

	V getEnsure(boolean ensure, Object... objects);

	public boolean isEmpty();

	public abstract <T> Collection<T> items(Object... objects);

	<T> Collection<T> keys(Object... objects);

	public <T> Set<T> keySet();

	public abstract Object put(Object... objects);

	void putMulti(MultikeyMap<V> multi);

	public abstract Object remove(Object... objects);

	public abstract <T> Collection<T> reverseItems(Object... objects);

	public abstract <T> Collection<T> reverseKeys(Object... objects);

	public abstract <T> Collection<T> reverseValues(Object... objects);

	void setDepth(int depth);

	public int size();

	public void sortKeys(Object... objects);

	public abstract void stripNonDuplicates(int depth);

	public abstract MultikeyMap<V> swapKeysZeroAndOne();

	default <T> Set<? extends T> typedKeySet(Class<T> clazz) {
		return keySet();
	}

	default <T> Collection<T> typedValues(Class<T> typed, Object... objects) {
		return values(objects);
	}

	public abstract <T> Collection<T> values(Object... objects);

	public abstract Map writeableDelegate();
}
