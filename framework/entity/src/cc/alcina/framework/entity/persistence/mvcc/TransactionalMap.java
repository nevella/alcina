package cc.alcina.framework.entity.persistence.mvcc;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MappingIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MultiIterator;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ObjectWrapper;
import it.unimi.dsi.fastutil.longs.Long2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

/*
 * 
 * This class is intended for transaction-aware indices and lookups. 
 * 
* It replaces TransactionalMapOld - the layer-based implementation did not scale during long-running txs 
 * ***(v2)
 * 
 * transitions through 1-1 :-> baseLayer :-> transactional
 *   
 */
public class TransactionalMap<K, V> extends AbstractMap<K, V>
		implements TransactionalCollection {
	private static transient final Object NULL_KEY_MARKER = new Object();

	private static transient final Object REMOVED_VALUE_MARKER = new Object();

	protected Class<K> keyClass;

	protected Class<V> valueClass;

	Comparator<K> keyComparator;

	private int hash = 0;

	/*
	 * Stores mappings in the base layer
	 */
	private Map<K, V> nonConcurrent;

	private SizeMetadata sizeMetadata;

	/*
	 * Non-generic because we use the NULL_KEY_MARKER - other than that; Map<K,
	 * Key<V>>
	 */
	private Map concurrent;

	public TransactionalMap(Class<K> keyClass, Class<V> valueClass) {
		this(keyClass, valueClass, null);
	}

	protected TransactionalMap(Class<K> keyClass, Class<V> valueClass,
			Comparator<K> keyComparator) {
		Preconditions.checkNotNull(keyClass);
		Preconditions.checkNotNull(valueClass);
		this.keyClass = keyClass;
		this.valueClass = valueClass;
		if (keyComparator != null) {
			this.keyComparator = new UnwrappingComparator(keyComparator);
		}
		nonConcurrent = createNonConcurrentMap();
	}

	@Override
	public boolean containsKey(Object key) {
		if (concurrent != null) {
			TransactionalValue transactionalValue = (TransactionalValue) concurrent
					.get(wrapTransactionalKey(key));
			if (transactionalValue != null) {
				return transactionalValue.isNotRemoved();
			}
		}
		return nonConcurrent.containsKey(key);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new EntrySetView();
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public V get(Object key) {
		if (concurrent != null) {
			TransactionalValue transactionalValue = (TransactionalValue) concurrent
					.get(wrapTransactionalKey(key));
			if (transactionalValue != null) {
				if (transactionalValue.isNotRemoved()) {
					return transactionalValue.getValue();
				} else {
					return null;
				}
			}
		}
		return nonConcurrent.get(key);
	}

	@Override
	/*
	 * Override, since we use these as keys for vacuum (and the cost would be
	 * outrageous for large maps)
	 */
	public int hashCode() {
		if (hash == 0) {
			hash = System.identityHashCode(this);
			if (hash == 0) {
				hash = -1;
			}
		}
		return hash;
	}

	public boolean isSorted() {
		return false;
	}

	@Override
	public Set<K> keySet() {
		return new KeySet();
	}

	@Override
	public V put(K key, V value) {
		Preconditions.checkArgument(
				key == null || keyClass.isAssignableFrom(key.getClass()));
		Preconditions.checkArgument(value == null || valueClass == Object.class
				|| valueClass.isAssignableFrom(value.getClass()));
		V existing = get(key);
		Transaction currentTransaction = Transaction.current();
		if (currentTransaction.isBaseTransaction()) {
			nonConcurrent.put(key, value);
		} else {
			boolean hasExisting = containsKey(key);
			ensureTransactional(currentTransaction);
			concurrent.computeIfAbsent(wrapTransactionalKey(key),
					k -> new TransactionalValue(key, ObjectWrapper.of(value),
							currentTransaction, true));
			TransactionalValue transactionalValue = (TransactionalValue) concurrent
					.get(wrapTransactionalKey(key));
			transactionalValue.put(value);
			if (!hasExisting) {
				sizeMetadata.delta(1);
			}
		}
		return existing;
	}

	@Override
	public V remove(Object key) {
		if (!containsKey(key)) {
			return null;
		}
		V existing = get(key);
		Transaction currentTransaction = Transaction.current();
		if (currentTransaction.isBaseTransaction()) {
			nonConcurrent.remove(key);
		} else {
			ensureTransactional(currentTransaction);
			concurrent.computeIfAbsent(wrapTransactionalKey(key),
					k -> new TransactionalValue((K) key,
							ObjectWrapper.of(REMOVED_VALUE_MARKER),
							currentTransaction, true));
			TransactionalValue transactionalValue = (TransactionalValue) concurrent
					.get(wrapTransactionalKey(key));
			transactionalValue.remove();
			sizeMetadata.delta(-1);
		}
		return existing;
	}

	@Override
	public int size() {
		return sizeMetadata == null ? nonConcurrent.size()
				: sizeMetadata.resolve(false).get();
	}

	@Override
	public String toString() {
		return Ax.format(
				"tx.map: %s=>%s : %s non-tx keys; %s tx keys : %s objects in this tx",
				keyClass.getSimpleName(), valueClass.getSimpleName(),
				nonConcurrent.size(), concurrent.size(), entrySet().size());
	}

	@Override
	public Collection<V> values() {
		if (Entity.class.isAssignableFrom(valueClass)) {
			return new ValuesSet();
		} else {
			return new ValuesCollection();
		}
	}

	private K unwrapTransactionalKey(Object key) {
		return key == NULL_KEY_MARKER ? null : (K) key;
	}

	private Object wrapTransactionalKey(Object key) {
		return key == null ? NULL_KEY_MARKER : key;
	}

	protected Map createConcurrentMap() {
		return new ConcurrentHashMap<>();
	}

	protected Map<K, V> createNonConcurrentMap() {
		if (keyClass == Long.class) {
			if (valueClass == Boolean.class) {
				return (Map<K, V>) new Long2BooleanLinkedOpenHashMap();
			} else {
				return (Map<K, V>) new Long2ObjectLinkedOpenHashMap<>();
			}
		} else {
			return (Map<K, V>) new Object2ObjectLinkedOpenHashMap<>();
		}
	}

	void ensureTransactional(Transaction currentTransaction) {
		if (concurrent == null) {
			synchronized (this) {
				if (concurrent == null) {
					concurrent = createConcurrentMap();
					sizeMetadata = new SizeMetadata(
							new AtomicInteger(nonConcurrent.size()),
							currentTransaction, true);
				}
			}
		}
	}

	/*
	 * Allow single-valued txsets to degenerate to txmaps
	 */
	void putInBaseLayer(Transaction baseTransaction, K key, V value) {
		nonConcurrent.put(key, value);
	}

	private class KeySet extends AbstractSet<K> {
		private Set<Entry<K, V>> entrySet;

		public KeySet() {
			this.entrySet = entrySet();
		}

		@Override
		public void clear() {
			entrySet.clear();
		}

		@Override
		public boolean contains(Object o) {
			return entrySet.contains(o);
		}

		@Override
		public Iterator<K> iterator() {
			return new MappingIterator<Entry<K, V>, K>(entrySet.iterator(),
					e -> e.getKey());
		}

		@Override
		public int size() {
			return entrySet.size();
		}

		@Override
		public Spliterator<K> spliterator() {
			return new UnsplittableIteratorSpliterator<>(iterator(), size());
		}
	}

	private class ValuesCollection extends AbstractCollection<V> {
		private Set<Entry<K, V>> entrySet;

		public ValuesCollection() {
			this.entrySet = entrySet();
		}

		@Override
		public Iterator<V> iterator() {
			return new MappingIterator<Entry<K, V>, V>(entrySet.iterator(),
					e -> e.getValue());
		}

		@Override
		public int size() {
			return entrySet.size();
		}

		@Override
		public Spliterator<V> spliterator() {
			return new UnsplittableIteratorSpliterator<>(iterator(), size());
		}
	}

	private class ValuesSet extends AbstractSet<V> {
		private Set<Entry<K, V>> entrySet;

		public ValuesSet() {
			this.entrySet = entrySet();
		}

		@Override
		public Iterator<V> iterator() {
			return new MappingIterator<Entry<K, V>, V>(entrySet.iterator(),
					e -> e.getValue());
		}

		@Override
		public int size() {
			return entrySet.size();
		}

		@Override
		public Spliterator<V> spliterator() {
			return new UnsplittableIteratorSpliterator<>(iterator(), size());
		}
	}

	class EntrySetView extends AbstractSet<Entry<K, V>> {
		private int size = -1;

		private Transaction transaction;

		public EntrySetView() {
			transaction = Transaction.current();
			if (transaction.isEnded()) {
				throw new MvccException(
						"Creating tx entry set outside of a transaction");
			}
		}

		@Override
		public void clear() {
			super.clear();
		}

		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			if (concurrent == null) {
				return nonConcurrent.entrySet().iterator();
			}
			Iterator<Entry<K, V>>[] iteratorArray = new Iterator[] {
					nonConcurrent.entrySet().iterator(),
					new TransactionalIterator() };
			MultiIterator<Entry<K, V>> layerIterator = new MultiIterator<Entry<K, V>>(
					false, keyComparator == null ? null
							: new Comparator<Entry<K, V>>() {
								@Override
								public int compare(Entry<K, V> o1,
										Entry<K, V> o2) {
									return keyComparator.compare(o1.getKey(),
											o2.getKey());
								}
							},
					iteratorArray);
			Predicate<Entry<K, V>> notVisibleFilter = e -> {
				Object key = wrapTransactionalKey(e.getKey());
				TransactionalValue transactionalValue = (TransactionalMap<K, V>.TransactionalValue) concurrent
						.get(key);
				if (transactionalValue != null) {
					return transactionalValue.isNotRemoved();
				}
				return true;
			};
			return new FilteringIterator<>(layerIterator, notVisibleFilter);
		}

		@Override
		//
		public int size() {
			if (size == -1) {
				size = estimateSize();
			}
			return size;
		}

		@Override
		public Spliterator<Entry<K, V>> spliterator() {
			return new UnsplittableIteratorSpliterator<>(iterator(),
					estimateSize());
		}

		private int estimateSize() {
			return TransactionalMap.this.size();
		}

		class TransactionalEntrySetEntry implements Entry<K, V> {
			public Entry entry;

			@Override
			public K getKey() {
				return unwrapTransactionalKey(entry.getKey());
			}

			@Override
			public V getValue() {
				return ((TransactionalValue) entry.getValue()).getValue();
			}

			@Override
			public V setValue(V value) {
				throw new UnsupportedOperationException();
			}
		}

		class TransactionalIterator implements Iterator<Entry<K, V>> {
			private Iterator<Entry> itr;

			private TransactionalEntrySetEntry entry;

			public TransactionalIterator() {
				itr = concurrent.entrySet().iterator();
				entry = new TransactionalEntrySetEntry();
			}

			@Override
			public boolean hasNext() {
				return itr.hasNext();
			}

			@Override
			public Entry<K, V> next() {
				entry.entry = itr.next();
				return entry;
			}
		}
	}

	class SizeMetadata extends MvccObjectVersions<AtomicInteger> {
		SizeMetadata(AtomicInteger t, Transaction initialTransaction,
				boolean initialObjectIsWriteable) {
			super(t, initialTransaction, initialObjectIsWriteable);
		}

		public void delta(int delta) {
			resolve(true).addAndGet(delta);
		}

		@Override
		protected boolean accessibleFromOtherTransactions(AtomicInteger t) {
			return false;
		}

		@Override
		protected AtomicInteger copyObject(AtomicInteger mostRecentObject) {
			return new AtomicInteger(mostRecentObject.get());
		}

		@Override
		protected void copyObject(AtomicInteger fromObject,
				AtomicInteger baseObject) {
			baseObject.set(fromObject.get());
		}

		@Override
		protected <E extends Entity> Class<E> entityClass() {
			return null;
		}

		@Override
		protected void onVersionCreation(AtomicInteger object) {
			// NOOP
		}

		@Override
		// TODO - strictly speaking, it possibly is - but the sizes returned are
		// only estimates
		protected boolean thisMayBeVisibleToPriorTransactions() {
			return false;
		}
	}

	class TransactionalValue extends MvccObjectVersions<ObjectWrapper> {
		private K key;

		TransactionalValue(K key, ObjectWrapper t,
				Transaction initialTransaction,
				boolean initialObjectIsWriteable) {
			super(t, initialTransaction, initialObjectIsWriteable);
			this.key = key;
		}

		public V getValue() {
			ObjectWrapper o = resolve(false);
			if (o == null) {
				/*
				 * No visible transaction
				 */
				return nonConcurrent.get(key);
			}
			Object value = o.get();
			if (value == REMOVED_VALUE_MARKER) {
				throw new UnsupportedOperationException();
			}
			return (V) o.get();
		}

		public boolean isNotRemoved() {
			ObjectWrapper o = resolve(false);
			if (o == null) {
				/*
				 * No visible transaction
				 */
				return nonConcurrent.containsKey(key);
			}
			Object value = o.get();
			if (value == REMOVED_VALUE_MARKER) {
				return false;
			}
			return true;
		}

		public void put(V value) {
			resolve(true).set(value);
		}

		public void remove() {
			resolve(true).set(REMOVED_VALUE_MARKER);
		}

		@Override
		protected boolean accessibleFromOtherTransactions(ObjectWrapper t) {
			return false;
		}

		@Override
		protected ObjectWrapper copyObject(ObjectWrapper mostRecentObject) {
			return ObjectWrapper.of(mostRecentObject.get());
		}

		@Override
		protected void copyObject(ObjectWrapper fromObject,
				ObjectWrapper baseObject) {
			baseObject.set(fromObject.get());
		}

		@Override
		protected <E extends Entity> Class<E> entityClass() {
			return null;
		}

		@Override
		protected void onVersionCreation(ObjectWrapper object) {
		}

		@Override
		protected boolean thisMayBeVisibleToPriorTransactions() {
			return true;
		}
	}

	static class UnsplittableIteratorSpliterator<E> implements Spliterator<E> {
		private Iterator<E> itr;

		private int size;

		UnsplittableIteratorSpliterator(Iterator<E> itr, int size) {
			this.itr = itr;
			this.size = size;
		}

		@Override
		public int characteristics() {
			return Spliterator.CONCURRENT;
		}

		@Override
		public long estimateSize() {
			return size;
		}

		@Override
		public boolean tryAdvance(Consumer<? super E> action) {
			if (itr.hasNext()) {
				action.accept(itr.next());
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Spliterator<E> trySplit() {
			return null;
		}
	}

	class UnwrappingComparator implements Comparator {
		private Comparator<K> comparator;

		public UnwrappingComparator(Comparator<K> keyComparator) {
			this.comparator = keyComparator;
		}

		@Override
		public int compare(Object o1, Object o2) {
			if (o1 == NULL_KEY_MARKER) {
				o1 = null;
			}
			if (o2 == NULL_KEY_MARKER) {
				o2 = null;
			}
			return comparator.compare((K) o1, (K) o2);
		}
	}
}
