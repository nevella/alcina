package cc.alcina.framework.entity.persistence.mvcc;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.InvariantOnceCreated;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MappingIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MultiIterator;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ObjectWrapper;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.VacuumableTransactions;
import it.unimi.dsi.fastutil.Hash;
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

	static Logger logger = LoggerFactory.getLogger(TransactionalTreeMap.class);

	protected Class<K> keyClass;

	protected Class<V> valueClass;

	Comparator<K> keyComparator;

	private int hash = 0;

	/*
	 * Stores mappings in the base layer
	 */
	protected Map<K, V> nonConcurrent;

	private SizeMetadata sizeMetadata;

	/*
	 * Non-generic because we use the NULL_KEY_MARKER - other than that; Map<K,
	 * TransactionalValue<V>>
	 */
	protected Map concurrent;

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

	public void debugNotFound(long id) {
		Logger logger = LoggerFactory.getLogger(getClass());
		V v = get(id);
		logger.info("debugNotFound - get - value {} - null: {}", v, v == null);
		v = nonConcurrent.get(id);
		logger.info("debugNotFound - nonConcurrent - value {} - null: {}", v,
				v == null);
		if (concurrent != null) {
			Object o = concurrent.get(id);
			logger.info("debugNotFound - concurrent - value {} - null: {}", o,
					o == null);
			if (o != null) {
				TransactionalValue value = (TransactionalValue) o;
				logger.info("debugNotFound - concurrent - txKey {} ",
						value.key);
				logger.info("debugNotFound - concurrent - txValue {} ", value);
				logger.info("debugNotFound - concurrent - isNotRemoved {} ",
						value.isNotRemoved());
				ObjectWrapper resolve = value.resolve(false);
				logger.info("debugNotFound - concurrent - txValue.resolve {} ",
						resolve);
				if (resolve != null) {
					logger.info(
							"debugNotFound - concurrent - txValue.resolve.get {} ",
							resolve.get());
				} else {
					logger.info(
							"debugNotFound - concurrent - txValue.resolve == null");
				}
			}
		}
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
			ensureConcurrent(currentTransaction);
			Object transactionalKey = wrapTransactionalKey(key);
			/*
			 * https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8161372
			 */
			TransactionalValue transactionalValue = (TransactionalValue) concurrent
					.get(transactionalKey);
			if (transactionalValue == null) {
				transactionalValue = (TransactionalValue) concurrent
						.computeIfAbsent(transactionalKey,
								k -> new TransactionalValue(key,
										ObjectWrapper.of(value),
										currentTransaction));
			}
			boolean success = transactionalValue.put(value);
			if (!success) {
				// concurrent vacuum - retry
				return put(key, value);
			}
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
			ensureConcurrent(currentTransaction);
			Object transactionalKey = wrapTransactionalKey(key);
			TransactionalValue transactionalValue = (TransactionalMap<K, V>.TransactionalValue) concurrent
					.get(transactionalKey);
			if (transactionalValue == null) {
				transactionalValue = (TransactionalMap<K, V>.TransactionalValue) concurrent
						.computeIfAbsent(transactionalKey,
								k -> new TransactionalValue((K) key,
										ObjectWrapper.of(REMOVED_VALUE_MARKER),
										currentTransaction));
			}
			transactionalValue.remove();
			sizeMetadata.delta(-1);
		}
		return existing;
	}

	@Override
	public int size() {
		if (sizeMetadata == null || sizeMetadata.resolve(false) == null) {
			return nonConcurrent.size();
		} else {
			return sizeMetadata.resolve(false).get();
		}
	}

	@Override
	public String toString() {
		return Ax.format(
				"tx.map: %s=>%s : %s non-tx keys; %s tx keys : %s objects in this tx",
				keyClass.getSimpleName(), valueClass.getSimpleName(),
				nonConcurrent.size(),
				(concurrent == null ? 0 : concurrent.size()),
				entrySet().size());
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

	protected Map createConcurrentMap() {
		return new ConcurrentHashMap<>();
	}

	protected Map<K, V> createNonConcurrentMap() {
		if (keyClass == Long.class) {
			if (valueClass == Boolean.class) {
				return (Map<K, V>) new Long2BooleanLinkedOpenHashMap(
						Hash.DEFAULT_INITIAL_SIZE, Hash.DEFAULT_LOAD_FACTOR);
			} else {
				return (Map<K, V>) new Long2ObjectLinkedOpenHashMap<>(
						Hash.DEFAULT_INITIAL_SIZE, Hash.DEFAULT_LOAD_FACTOR);
			}
		} else {
			return (Map<K, V>) new Object2ObjectLinkedOpenHashMap<>(
					Hash.DEFAULT_INITIAL_SIZE, Hash.DEFAULT_LOAD_FACTOR);
		}
	}

	protected Object wrapTransactionalKey(Object key) {
		return key == null ? NULL_KEY_MARKER : key;
	}

	void ensureConcurrent(Transaction currentTransaction) {
		if (concurrent == null) {
			synchronized (this) {
				if (concurrent == null) {
					sizeMetadata = new SizeMetadata(
							new AtomicInteger(nonConcurrent.size()),
							currentTransaction);
					concurrent = createConcurrentMap();
				}
			}
		}
		if (sizeMetadata == null) {
			synchronized (this) {
				// force wait for other sync block (this is needed because
				// instructions in the 'if (concurrent == null) {' block may be
				// out of order - wanted to avoid volatile access since that
				// affects performance
				if (sizeMetadata == null) {
					throw new RuntimeException("Should not be null");
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

	public static class EntityIdMap extends TransactionalMap<Long, Entity> {
		public EntityIdMap(Class<Long> keyClass, Class<Entity> valueClass) {
			super(keyClass, valueClass);
		}

		public void ensureVersion(Long key) {
			if (concurrent != null) {
				TransactionalValue transactionalValue = (TransactionalValue) concurrent
						.get(wrapTransactionalKey(key));
				if (transactionalValue != null) {
					transactionalValue.ensureVersion();
				}
			}
		}

		public Entity getAnyTransaction(Long key) {
			if (concurrent != null) {
				TransactionalValue transactionalValue = (TransactionalValue) concurrent
						.get(wrapTransactionalKey(key));
				if (transactionalValue != null) {
					return transactionalValue.getAnyTransaction();
				}
			}
			return nonConcurrent.get(key);
		}
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
			TransactionalIterator transactionalIterator = new TransactionalIterator();
			Iterator<Entry<K, V>>[] iteratorArray = new Iterator[] {
					nonConcurrent.entrySet().iterator(),
					transactionalIterator };
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
				boolean inTransactionalIteratorPhase = layerIterator
						.getCurrentIterator()
						.getSource() == transactionalIterator;
				if (transactionalValue != null) {
					return inTransactionalIteratorPhase
							&& transactionalValue.isNotRemoved();
				} else {
					/*
					 * if null and in tx iterator phase, the key was vacuumed
					 */
					return !inTransactionalIteratorPhase;
				}
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
		SizeMetadata(AtomicInteger t, Transaction initialTransaction) {
			super(t, initialTransaction, false);
		}

		public void delta(int delta) {
			resolve(true).addAndGet(delta);
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
		protected AtomicInteger
				initialAllTransactionsValueFor(AtomicInteger t) {
			return new AtomicInteger(0);
		}

		@Override
		protected boolean thisMayBeVisibleToPriorTransactions() {
			return true;
		}
	}

	class TransactionalValue extends MvccObjectVersions<ObjectWrapper> {
		private K key;

		TransactionalValue(K key, ObjectWrapper t,
				Transaction initialTransaction) {
			super(t, initialTransaction, false);
			this.key = key;
		}

		public void ensureVersion() {
			if (get(key) != null) {
				return;
			}
			V value = getAnyTransaction();
			put(value);
		}

		public V getValue() {
			ObjectWrapper o = resolve(false);
			if (o == null) {
				/*
				 * No visible transaction
				 */
				return nonConcurrent.get(key);
			}
			// not-not - but otherwise so many single nots
			if (!isNotRemovedValueMarker(o)) {
				throw new UnsupportedOperationException();
			}
			return (V) o.get();
		}

		@Override
		public int hashCode() {
			if (key instanceof Entity) {
				return Objects.hash(key);
			}
			if (key instanceof Long && ((Long) key).longValue() != 0) {
				return Objects.hash(key);
			}
			return super.hashCode();
		}

		public boolean isNotRemoved() {
			ObjectWrapper o = resolve(false);
			if (o == null) {
				/*
				 * No visible transaction
				 */
				return nonConcurrent.containsKey(key);
			}
			return isNotRemovedValueMarker(o);
		}

		public void remove() {
			resolve(true).set(REMOVED_VALUE_MARKER);
		}

		@Override
		public void vacuum(VacuumableTransactions vacuumableTransactions) {
			super.vacuum(vacuumableTransactions);
			synchronized (this) {
				if (getSize() == 0 && visibleAllTransactions
						.get() == REMOVED_VALUE_MARKER) {
					if (!nonConcurrent.containsKey(key)) {
						/*
						 * Note that this only affects objects not loaded in the
						 * base transaction - but that's generally exactly (viz
						 * lazy-loaded objects) what we'd want to vacuum anyway
						 */
						concurrent.remove(wrapTransactionalKey(key));
					}
				}
			}
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
		/*
		 * because possibly visible before version creation, the base must be
		 * marked as 'removed'
		 */
		protected ObjectWrapper
				initialAllTransactionsValueFor(ObjectWrapper t) {
			return ObjectWrapper.of(REMOVED_VALUE_MARKER);
		}

		@Override
		protected boolean thisMayBeVisibleToPriorTransactions() {
			return true;
		}

		V getAnyTransaction() {
			ObjectWrapper baseObject = visibleAllTransactions;
			if (isNotRemovedValueMarker(baseObject)) {
				return (V) baseObject.get();
			}
			return (V) versions.values().stream()
					.filter(ov -> isNotRemovedValueMarker(ov.object))
					.findFirst().get().object.get();
		}

		boolean isNotRemovedValueMarker(ObjectWrapper o) {
			return o.get() != REMOVED_VALUE_MARKER;
		}

		boolean put(V value) {
			/*
			 * this synchronization may or may not be spendy - I have a feeling
			 * not (compared to resolve) since concurrent access should be very
			 * rare
			 */
			synchronized (this) {
				if (concurrent.get(wrapTransactionalKey(key)) == null) {
					// vacuumed
					return false;
				}
				resolve(true).set(value);
				return true;
			}
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
			if (o1 instanceof Entity) {
				Entity entity = (Entity) o1;
				MvccObjectVersions mvccVersions = ((MvccObject) entity)
						.__getMvccVersions__();
				// there's no easy way to get rid of unreachable entities in the
				// sortedmap - but given they should be invariant we just use
				// their while-alive values
				if (mvccVersions != null
						&& mvccVersions.hasNoVisibleTransaction()) {
					if (entity instanceof InvariantOnceCreated) {
						if (entity.getId() == 0) {
							mvccVersions.resolveInvariantToDomainIdentity();
						}
					} else {
						logger.warn(
								"Cpr unreachable access - non invariant entity - {}",
								entity.getClass().getSimpleName());
					}
				}
			}
			if (o2 instanceof Entity) {
				Entity entity = (Entity) o2;
				MvccObjectVersions mvccVersions = ((MvccObject) entity)
						.__getMvccVersions__();
				if (mvccVersions != null
						&& mvccVersions.hasNoVisibleTransaction()) {
					if (entity instanceof InvariantOnceCreated) {
						if (entity.getId() == 0) {
							mvccVersions.resolveInvariantToDomainIdentity();
						}
					} else {
						logger.warn(
								"Cpr unreachable access - non invariant entity - {}",
								entity.getClass().getSimpleName());
					}
				}
			}
			return comparator.compare((K) o1, (K) o2);
		}
	}
}
