package cc.alcina.framework.entity.persistence.mvcc;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.InvariantOnceCreated;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MappingIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MultiIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.UnboxedLongMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.VacuumableTransactions;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

/*
 *
 * This class is intended for transaction-aware indices and lookups.
 *
 * It replaces TransactionalMapOld - the layer-based implementation did not
 * scale during long-running txs ***(v2)
 *
 * transitions through 1-1 :-> baseLayer :-> transactional
 *
 */
public class TransactionalMap<K, V> extends AbstractMap<K, V>
		implements TransactionalCollection, UnboxedLongMap<V> {
	private static transient final NullKeyMarker NULL_KEY_MARKER = new NullKeyMarker();

	private static transient final RemovedValueMarker REMOVED_VALUE_MARKER = new RemovedValueMarker();

	static Logger logger = LoggerFactory.getLogger(TransactionalMap.class);

	protected Class<K> keyClass;

	protected Class<V> valueClass;

	Comparator<K> keyComparator;

	boolean pureTransactional;

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
	protected volatile Map concurrent;

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

	protected boolean applyToBase(Transaction currentTransaction) {
		return !pureTransactional && currentTransaction.isBaseTransaction();
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

	public Spliterator createSpliterator(Iterator iterator, int size) {
		return Spliterators.spliterator(iterator, size, Spliterator.CONCURRENT);
	}

	public void debugNotFound(long id) {
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
				Ref resolve = value.resolve(false);
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
		// the below *looks* questionable but works and, I think, fixed a race
		// bug. There may be a better way to do it (exit of the preceding sync
		// block should ensure both sizeMetadata and concurrent are non null,
		// IMO) - the following block is harmless in any event
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

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new EntrySetView();
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public V get(long key) {
		// this optimisation means that during app startup, get() calls don't
		// cause boxing. Once app is started up (well, the map has been changed
		// outside the startup transaction), boxing is required
		if (concurrent != null) {
			return get((Object) key);
		} else {
			return ((Long2ObjectFunction<V>) nonConcurrent).get(key);
		}
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
		if (applyToBase(currentTransaction)) {
			nonConcurrent.put(key, value);
		} else {
			// note this is transactional (as is size)
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
						.computeIfAbsent(transactionalKey, k -> {
							return new TransactionalValue(key, Ref.of(value),
									currentTransaction);
						});
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

	/*
	 * Allow single-valued txsets to degenerate to txmaps
	 */
	protected void putInBaseLayer(Transaction baseTransaction, K key, V value) {
		nonConcurrent.put(key, value);
	}

	@Override
	public V remove(Object key) {
		if (!containsKey(key)) {
			return null;
		}
		V existing = get(key);
		Transaction currentTransaction = Transaction.current();
		if (applyToBase(currentTransaction)) {
			nonConcurrent.remove(key);
		} else {
			ensureConcurrent(currentTransaction);
			Object transactionalKey = wrapTransactionalKey(key);
			TransactionalValue transactionalValue = (TransactionalMap<K, V>.TransactionalValue) concurrent
					.get(transactionalKey);
			Ref<Boolean> createdTransactionalValue = new Ref<>();
			createdTransactionalValue.set(false);
			if (transactionalValue == null) {
				transactionalValue = (TransactionalMap<K, V>.TransactionalValue) concurrent
						.computeIfAbsent(transactionalKey, k -> {
							createdTransactionalValue.set(true);
							return new TransactionalValue((K) key,
									Ref.of(REMOVED_VALUE_MARKER),
									currentTransaction);
						});
			}
			// if, prior to this call the key only existed in the
			// nonConcurrent map, createdTransactionalValue.get() will return
			// true and transactionalValue.remove() false - this handles the
			// case where the key only exists in nonConcurrent prior to this
			// call
			boolean removed = transactionalValue.remove()
					|| createdTransactionalValue.get().booleanValue();
			if (removed) {
				int size = sizeMetadata.delta(-1);
			}
		}
		return existing;
	}

	@Override
	public int size() {
		if (sizeMetadata == null || sizeMetadata.resolve(false) == null) {
			return nonConcurrent.size();
		} else {
			int fromSizeMeta = sizeMetadata.resolve(false).get();
			if (fromSizeMeta < 0) {
				logger.warn("DEVEX-01 - illegal size");
			}
			return fromSizeMeta;
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

	private K unwrapTransactionalKey(Object key) {
		return key == NULL_KEY_MARKER ? null : (K) key;
	}

	@Override
	public Collection<V> values() {
		if (Entity.class.isAssignableFrom(valueClass)) {
			return new ValuesSet();
		} else {
			return new ValuesCollection();
		}
	}

	protected Object wrapTransactionalKey(Object key) {
		return key == null ? NULL_KEY_MARKER : key;
	}

	public static class EntityIdMap extends TransactionalMap<Long, Entity> {
		public EntityIdMap(Class<Long> keyClass, Class<Entity> valueClass) {
			super(keyClass, valueClass);
		}

		private void checkLegal(Long key, Entity value) {
			boolean illegal = value.getId() != 0
					&& (key.longValue() != value.getId());
			illegal |= value.getId() == 0
					&& (key.longValue() != value.getLocalId());
			if (illegal) {
				throw new IllegalArgumentException(
						Ax.format("Inserting with incorrect id: %s %s", key,
								value.toLocator()));
			}
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

		@Override
		public Entity put(Long key, Entity value) {
			checkLegal(key, value);
			return super.put(key, value);
		}

		@Override
		protected void putInBaseLayer(Transaction baseTransaction, Long key,
				Entity value) {
			checkLegal(key, value);
			super.putInBaseLayer(baseTransaction, key, value);
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

		private int estimateSize() {
			return TransactionalMap.this.size();
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
			boolean debugNegativeIds = TransactionalMap.this instanceof EntityIdMap;
			Predicate<Entry<K, V>> notVisibleFilter = e -> {
				Object key = wrapTransactionalKey(e.getKey());
				if (debugNegativeIds && key instanceof Long) {
					if (((Long) key).longValue() < 0) {
						logger.warn("Negative key :: {}", key);
					}
				}
				TransactionalValue transactionalValue = (TransactionalMap<K, V>.TransactionalValue) concurrent
						.get(key);
				boolean inTransactionalIteratorPhase = layerIterator
						.getCurrentIterator()
						.getSource() == transactionalIterator;
				// initial value not used
				boolean visible = false;
				if (transactionalValue != null) {
					if (inTransactionalIteratorPhase) {
						boolean removed = transactionalValue.isRemoved();
						visible = !removed;
					} else {
						// not visible during nonTransactionalPhase (since the
						// 'real' value visible to this tx is the value
						// reachable
						// via field 'concurrent'
						visible = false;
					}
				} else {
					if (inTransactionalIteratorPhase) {
						/*
						 * if null and in tx iterator phase, the key was
						 * vacuumed (during iteration over field 'concurrent')
						 */
						visible = false;
					} else {
						// most cases (created during domain warmup, unmodified)
						// branch here
						visible = true;
					}
				}
				return visible;
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
			return createSpliterator(iterator(), estimateSize());
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
			return createSpliterator(iterator(), size());
		}
	}

	private static class NullKeyMarker {
	}

	private static class RemovedValueMarker {
	}

	/*
	 * From memory. access to version.value is tx-safe (so we could use an
	 * integer, not atomicinteger), but atomicinteger is a convenient container
	 * for a mutable int
	 */
	class SizeMetadata extends MvccObjectVersions<AtomicInteger> {
		SizeMetadata(AtomicInteger t, Transaction initialTransaction) {
			super(t, initialTransaction, pureTransactional, null);
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

		public int delta(int delta) {
			return resolve(true).addAndGet(delta);
		}

		@Override
		protected AtomicInteger initialAllTransactionsValueFor(AtomicInteger t,
				Object context, boolean baseTransaction) {
			/*
			 * All txs begin with the size of the nonconcurrent map
			 */
			return baseTransaction ? domainIdentity
					: new AtomicInteger(t.get());
		}

		@Override
		protected boolean mayBeReachableFromPreCreationTransactions() {
			return true;
		}
	}

	class TransactionalValue extends MvccObjectVersions<Ref> {
		private K key;

		TransactionalValue(K key, Ref t, Transaction initialTransaction) {
			super(t, initialTransaction, false, key);
			this.key = key;
		}

		@Override
		protected Ref copyObject(Ref mostRecentObject) {
			return Ref.of(mostRecentObject.get());
		}

		@Override
		protected void copyObject(Ref fromObject, Ref baseObject) {
			baseObject.set(fromObject.get());
		}

		public void ensureVersion() {
			if (get(key) != null) {
				return;
			}
			// won't be vacuumed (since tx is live) so no need to synchronize
			V value = getAnyTransaction();
			put(value);
		}

		/*
		 * synchronized: must block vacuum() calls
		 */
		synchronized V getAnyTransaction() {
			Ref baseObject = visibleAllTransactions;
			if (notRemovedValueMarker(baseObject)) {
				return (V) baseObject.get();
			}
			Optional<ObjectVersion<Ref>> version = versions().values().stream()
					.filter(objectVersion -> notRemovedValueMarker(
							objectVersion.object))
					.findFirst();
			if (version.isEmpty()) {
				throw new IllegalStateException(Ax.format(
						"getAnyTransaction - no non-removed value (vacuum race?): %s",
						key));
			}
			return (V) version.get().object.get();
		}

		public V getValue() {
			Ref o = resolve(false);
			if (o == null) {
				/*
				 * No visible transaction
				 */
				return nonConcurrent.get(key);
			}
			if (isRemovedValueMarker(o)) {
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

		@Override
		protected Ref initialAllTransactionsValueFor(Ref t, Object context,
				boolean baseTransaction) {
			// (if in baseTransaction/pureTransaction mode)
			if (baseTransaction) {
				return domainIdentity;
			} else {
				/*
				 * because possibly visible before version creation, the
				 * all-visible value must be either 'removed' (if non-existent
				 * in the base tx), or the base tx value
				 */
				// called before this.key is populated
				K key = (K) context;
				if (nonConcurrent.containsKey(key)) {
					V nonTransactionalValue = nonConcurrent.get(key);
					return Ref.of(nonTransactionalValue);
				} else {
					return Ref.of(REMOVED_VALUE_MARKER);
				}
			}
		}

		public boolean isNotRemoved() {
			Ref o = resolve(false);
			if (o == null) {
				/*
				 * No visible transaction
				 */
				return nonConcurrent.containsKey(key);
			}
			return notRemovedValueMarker(o);
		}

		boolean isRemoved() {
			return !isNotRemoved();
		}

		boolean isRemovedValueMarker(Ref o) {
			return o.get() == REMOVED_VALUE_MARKER;
		}

		@Override
		protected boolean mayBeReachableFromPreCreationTransactions() {
			return true;
		}

		boolean notRemovedValueMarker(Ref o) {
			return !isRemovedValueMarker(o);
		}

		synchronized boolean put(V value) {
			if (concurrent.get(wrapTransactionalKey(key)) == null) {
				// vacuumed
				return false;
			}
			resolve(true).set(value);
			return true;
		}

		/**
		 * return true if changed
		 */
		public boolean remove() {
			Ref resolved = resolve(true);
			if (notRemovedValueMarker(resolved)) {
				resolved.set(REMOVED_VALUE_MARKER);
				return true;
			} else {
				// Note that this shouldn't normally happen
				// FIXME - mvcc.4 - log
				return false;
			}
		}

		@Override
		protected void vacuum0(VacuumableTransactions vacuumableTransactions) {
			super.vacuum0(vacuumableTransactions);
			if (getSize() == 0
					&& visibleAllTransactions.get() == REMOVED_VALUE_MARKER) {
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
						mvccVersions.resolveInvariantToDomainIdentity();
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
						mvccVersions.resolveInvariantToDomainIdentity();
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
			return createSpliterator(iterator(), size());
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
					e -> {
						V value = e.getValue();
						if (value instanceof Entity) {
							if (((Entity) value).getId() < 0) {
								logger.warn(
										"Entity with negative id in iterated tx map:: {}",
										value);
							}
						}
						return value;
					});
		}

		@Override
		public int size() {
			return entrySet.size();
		}

		@Override
		public Spliterator<V> spliterator() {
			return createSpliterator(iterator(), size());
		}
	}
}
