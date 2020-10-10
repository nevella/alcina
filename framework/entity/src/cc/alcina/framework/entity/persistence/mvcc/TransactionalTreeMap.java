package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;

import it.unimi.dsi.fastutil.longs.Long2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;

public class TransactionalTreeMap<K, V> extends TransactionalMap<K, V>
		implements NavigableMap<K, V> {
	public TransactionalTreeMap(Class<K> keyClass, Class<V> valueClass,
			Comparator<K> comparator) {
		super(keyClass, valueClass);
		this.comparator = comparator;
		createBaseLayer();
	}

	@Override
	public Entry<K, V> ceilingEntry(K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public K ceilingKey(K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Comparator<? super K> comparator() {
		return comparator;
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NavigableMap<K, V> descendingMap() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> firstEntry() {
		throw new UnsupportedOperationException();
	}

	@Override
	public K firstKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> floorEntry(K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public K floorKey(K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedMap<K, V> headMap(K toKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> higherEntry(K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public K higherKey(K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSorted() {
		return true;
	}

	@Override
	public Entry<K, V> lastEntry() {
		throw new UnsupportedOperationException();
	}

	@Override
	public K lastKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> lowerEntry(K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public K lowerKey(K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NavigableSet<K> navigableKeySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> pollFirstEntry() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> pollLastEntry() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
			boolean toInclusive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected <V1> Map<K, V1> createNonSynchronizedMap(Class<V1> valueClass) {
		if (keyClass == Long.class) {
			if (valueClass == Boolean.class) {
				return (Map<K, V1>) new Long2BooleanAVLTreeMap(
						(Comparator<? super Long>) comparator);
			} else {
				return (Map<K, V1>) new Long2ObjectAVLTreeMap<>(
						(Comparator<? super Long>) comparator);
			}
		} else {
			return (Map<K, V1>) new Object2ObjectAVLTreeMap<>(comparator);
		}
	}

	@Override
	protected void init() {
		// super.init();
		// noop - need to set comparator in constructor first
	}
}
