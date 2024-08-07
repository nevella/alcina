package cc.alcina.framework.entity.persistence.mvcc;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MappingIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MultiIterator;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.Vacuumable;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.VacuumableTransactions;
import cc.alcina.framework.entity.projection.GraphProjection;
import it.unimi.dsi.fastutil.longs.Long2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * 
 * This class is intended for transaction-aware indices and lookups. A 'layer'
 * records deltas to the map state by recording modifications and removals in
 * separate, non-locked submaps.
 * 
 * The map is composed of a base layer, a list of visible-to-all-transactions
 * merged layers and a list of current transaction layers whose visibility is
 * transaction-dependent.
 * 
 * The merged layers are generated during vacuum, and are intended to avoid
 * runaway map layers for heavily written maps by using the levelled compaction
 * algorithm similar to that used in Cassandra and Chromium leveldb compactions.
 * 
 * 
 * Note that vacuum is probably the most interesting part of this class - it's
 * like sliding plates (B=merged A1, A2) under other plates (A1, A2) - it would
 * be nice to formally prove that the combination (B,A2) === (B) === (B,A1,A2)
 * (in layer order)
 * 
 * 
 * Operations are resolved to return the most recent value for the combined
 * sequence of layers, from (per-transaction) youngest-to-oldest
 * 
 * This class allows null keys and values
 * 
 * Synchronization:
 * 
 * 'this' acts as a lock on creation addition/removal of a layer (after base
 * transaction)
 * 
 * 
 * Deprecated - replaced by TransactionalMap which uses a simpler tx resolution
 * strategy and has better (and non-falling-off-a-cliff-in-worst-case)
 * performance. Of historical interest only
 */
@Deprecated
public class TransactionalMapOld<K, V> extends AbstractMap<K, V>
		implements Vacuumable, TransactionalCollection {
	private Layer base;

	private Layers layers;

	protected Class<K> keyClass;

	private Class<V> valueClass;

	Comparator<K> comparator;

	private boolean immutableValues;

	private int hash = 0;

	public TransactionalMapOld(Class<K> keyClass, Class<V> valueClass) {
		Preconditions.checkNotNull(keyClass);
		Preconditions.checkNotNull(valueClass);
		this.keyClass = keyClass;
		this.valueClass = valueClass;
		init();
	}

	@Override
	public boolean containsKey(Object key) {
		if (layers == null) {
			return base.wasRemoved(key) ? false : base.wasModified(key);
		}
		List<Layer> visibleLayers = visibleLayers();
		for (int idx = visibleLayers.size() - 1; idx >= 0; idx--) {
			Layer layer = visibleLayers.get(idx);
			if (layer.wasRemoved(key)) {
				return false;
			}
			if (layer.wasModified(key)) {
				return true;
			}
		}
		return false;
	}

	protected void createBaseLayer() {
		Preconditions.checkState(base == null);
		base = new Layer(Transaction.current());
	}

	protected <V1> Map<K, V1> createNonSynchronizedMap(Class<V1> valueClass) {
		if (keyClass == Long.class) {
			if (valueClass == Boolean.class) {
				return (Map<K, V1>) new Long2BooleanLinkedOpenHashMap();
			} else {
				return (Map<K, V1>) new Long2ObjectLinkedOpenHashMap<>();
			}
		} else {
			return (Map<K, V1>) new Object2ObjectLinkedOpenHashMap<>();
		}
	}

	private TransactionalMapOld<K, V>.Layer ensureLayer() {
		Transaction transaction = Transaction.current();
		if (transaction.isBaseTransaction()) {
			return base;
		}
		if (layers == null || !layers.nonMergedTransactionLayers
				.containsKey(transaction)) {
			// synchronize both to avoid double-initial-layers creation and
			// conflict with vacuum swapping of the layers object
			synchronized (this) {
				if (layers == null) {
					layers = new Layers();
					layers.mergedLayerList.add(base);
				}
				layers.nonMergedTransactionLayers.put(transaction,
						new Layer(transaction));
				Transactions.get().onAddedVacuumable(transaction, this);
			}
		}
		return layers.nonMergedTransactionLayers.get(transaction);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new TransactionalEntrySet();
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public V get(Object key) {
		if (layers == null) {
			return base.wasRemoved(key) ? null : base.get(key);
		}
		List<Layer> visibleLayers = visibleLayers();
		for (int idx = visibleLayers.size() - 1; idx >= 0; idx--) {
			Layer layer = visibleLayers.get(idx);
			if (layer.wasRemoved(key)) {
				return null;
			}
			if (layer.wasModified(key)) {
				return layer.get(key);
			}
		}
		return null;
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

	protected void init() {
		createBaseLayer();
	}

	/*
	 * Signifies that the value, once set, will never be changed. This is not
	 * checked explicitly
	 */
	public boolean isImmutableValues() {
		return this.immutableValues;
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
		Layer layer = ensureLayer();
		layer.put(key, value, containsKey(key));
		return existing;
	}

	/*
	 * Allow single-valued txsets to degenerate to txmaps
	 */
	void putInBaseLayer(Transaction baseTransaction, K key, V value) {
		base.transaction = baseTransaction;
		base.put(key, value, false);
	}

	@Override
	public V remove(Object key) {
		if (!containsKey(key)) {
			return null;
		}
		V existing = get(key);
		Layer layer = ensureLayer();
		layer.remove((K) key);
		return existing;
	}

	public void setImmutableValues(boolean immutableValues) {
		this.immutableValues = immutableValues;
	}

	@Override
	public String toString() {
		return Ax.format("tx.map: %s=>%s : %s layers : %s objects in this tx",
				keyClass.getSimpleName(), valueClass.getSimpleName(),
				layers == null ? 1 : layers.size(), entrySet().size());
	}

	@Override
	public void vacuum(VacuumableTransactions vacuumableTransactions) {
		layers.nonMergedTransactionLayers.keySet().removeAll(
				vacuumableTransactions.completedNonDomainTransactions);
		List<Transaction> commonVisible = TransactionVersions.commonVisible(
				vacuumableTransactions.completedDomainTransactions,
				layers.nonMergedTransactionLayers.keySet());
		if (commonVisible.isEmpty()) {
			// will not affect visible layers to any current transactionm no
			// sync needed
			return;
		}
		Collections.reverse(commonVisible);
		Layers mergedReplaceLayers = new Layers();
		List<Layer> mergedReplace = mergedReplaceLayers.mergedLayerList;
		mergedReplace.addAll(layers.mergedLayerList);
		commonVisible.stream().map(layers.nonMergedTransactionLayers::get)
				.forEach(mergedReplace::add);
		/*
		 * now compact - this is a 'levelled' strategy a la cassandra, chrome
		 * etc - layer iteration cost is ~ log10(n) and copy cost is ~
		 * n.log10(n) for n = total delta size
		 */
		boolean modified = false;
		do {
			modified = false;
			for (int idx = 0; idx < mergedReplace.size() - 1; idx++) {
				Layer layer0 = mergedReplace.get(idx);
				Layer layer1 = mergedReplace.get(idx + 1);
				if (layer1.combinedMapSize() * 10 > layer0.combinedMapSize()) {
					Layer merged = layer0.merge(layer1, mergedReplace, idx - 1);
					mergedReplace.remove(idx);
					// not idx+1 - we've just removed idx (but essentially
					// removing both old layers)
					mergedReplace.remove(idx);
					mergedReplace.add(idx, merged);
					modified = true;
					break;
				}
			}
		} while (modified);
		synchronized (this) {
			mergedReplaceLayers.nonMergedTransactionLayers
					.putAll(layers.nonMergedTransactionLayers);
			mergedReplaceLayers.nonMergedTransactionLayers.keySet()
					.removeAll(commonVisible);
			this.layers = mergedReplaceLayers;
		}
	}

	@Override
	public Collection<V> values() {
		if (Entity.class.isAssignableFrom(valueClass)) {
			return new ValuesSet();
		} else {
			return new ValuesCollection();
		}
	}

	/*
	 */
	private List<Layer> visibleLayers() {
		if (layers == null) {
			return Collections.singletonList(base);
		} else {
			return layers.visibleToCurrentTx();
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
	}

	/*
	 * // trickiest thing about this class is actually keeping track of size a
	 * la delta in this layer
	 * 
	 * delta is size("key in modified, not in containsKey() of older layers") -
	 * size(wasRemoved)
	 * 
	 * if key is in modified,
	 * 
	 */
	class Layer {
		private int added;

		private Map<K, Boolean> removed;

		private Map<K, V> modified = createNonSynchronizedMap(valueClass);

		private Transaction transaction;

		public Layer(Transaction transaction) {
			this.transaction = transaction;
		}

		int combinedMapSize() {
			return (removed == null ? 0 : removed.size()) + modified.size();
		}

		private Map<K, Boolean> ensureRemoved() {
			if (removed == null) {
				removed = createNonSynchronizedMap(Boolean.class);
			}
			return removed;
		}

		public V get(Object key) {
			return modified.get(key);
		}

		private boolean hasRemoved() {
			return removed != null;
		}

		/*
		 * returns a new non-live-transaction layer containing the union of
		 * these two
		 * 
		 * note that some variants of fastutil maps don't support
		 * keySet().remove(), hence fancy remove loops
		 */
		public Layer merge(Layer newerLayer, List<Layer> olderLayers,
				int newestOlderLayerIndex) {
			Layer merged = new Layer(null);
			if (hasRemoved()) {
				merged.ensureRemoved().putAll(ensureRemoved());
				newerLayer.modified.keySet()
						.forEach(k -> merged.ensureRemoved().remove(k));
			}
			if (newerLayer.hasRemoved()) {
				merged.ensureRemoved().putAll(newerLayer.ensureRemoved());
			}
			merged.modified.putAll(modified);
			merged.modified.putAll(newerLayer.modified);
			if (merged.hasRemoved()) {
				merged.ensureRemoved().keySet()
						.forEach(merged.modified::remove);
			}
			/*
			 * now ... how many of merged.modified were in fact added?
			 * 
			 * Other way to do this would be to have three (lazy) maps - added,
			 * modified, removed - per layer
			 * 
			 * We also clean up removed - if not contained in older layers, just
			 * remove it
			 */
			for (K key : merged.modified.keySet()) {
				boolean added = true;
				for (int idx = newestOlderLayerIndex; idx >= 0; idx--) {
					Layer layer = olderLayers.get(idx);
					if (layer.wasRemoved(key)) {
						break;// yep, added
					}
					if (layer.wasModified(key)) {
						added = false;
						break;// nope
					}
				}
				if (added) {
					merged.added++;
				}
			}
			if (merged.hasRemoved()) {
				for (Iterator<K> itr = merged.removed.keySet().iterator(); itr
						.hasNext();) {
					K key = itr.next();
					boolean drop = true;
					for (int idx = newestOlderLayerIndex; idx >= 0; idx--) {
						Layer layer = olderLayers.get(idx);
						if (layer.wasRemoved(key)) {
							break;// will have no effect on combined operations;
									// drop
						}
						if (layer.wasModified(key)) {
							drop = false;
							break;// will have an effect on combined operations
						}
					}
					if (drop) {
						itr.remove();
					}
				}
			}
			return merged;
		}

		Iterator<Entry<K, V>> modifiedEntrySetIterator() {
			Iterator<Entry<K, V>> iterator = modified.entrySet().iterator();
			return iterator;
		}

		public void put(K key, V value, boolean existing) {
			modified.put(key, value);
			if (!existing) {
				Boolean wasInRemoved = null;
				if (hasRemoved()) {
					wasInRemoved = ensureRemoved().remove(key);
				}
				if (wasInRemoved == null) {
					added++;
				} else {
					// effective size will increment because removed from
					// removed...
				}
			}
		}

		public void remove(K key) {
			ensureRemoved().put(key, Boolean.TRUE);
			modified.remove(key);
		}

		@Override
		public String toString() {
			FormatBuilder fb = new FormatBuilder();
			fb.line("tx:%s", transaction);
			fb.line("%s", GraphProjection.fieldwiseToString(this, false, false,
					999, "transaction", "this$0"));
			return fb.toString();
		}

		public boolean wasModified(Object key) {
			return modified.containsKey(key);
		}

		public boolean wasModifiedOrRemoved(Object key) {
			return (removed != null && removed.containsKey(key))
					|| modified.containsKey(key);
		}

		public boolean wasRemoved(Object key) {
			return removed != null && removed.containsKey(key);
		}
	}

	private class Layers {
		ConcurrentSkipListMap<Transaction, Layer> nonMergedTransactionLayers = new ConcurrentSkipListMap<>(
				Collections.reverseOrder());

		/*
		 * Not changed for the lifetime of this Layers instance (vacuum computes
		 * a new layers object and swaps TransactionalMapOld.layers)
		 */
		List<Layer> mergedLayerList = new ArrayList<>();

		public int size() {
			return nonMergedTransactionLayers.size() + mergedLayerList.size();
		}

		// FIXME - possibly cache this?
		public List<Layer> visibleToCurrentTx() {
			Transaction current = Transaction.current();
			// currentLayer will only be non-null if this TxMap has been
			// modified by the current tx
			Layer currentLayer = nonMergedTransactionLayers.get(current);
			// best case - no changes in this tx, and no visible unvacuumed txs
			if (current.committedTransactions.isEmpty()
					&& currentLayer == null) {
				return mergedLayerList;
			}
			List<Transaction> visibleCommittedTransactions = current
					.visibleCommittedTransactions(
							nonMergedTransactionLayers.keySet());
			if (visibleCommittedTransactions.isEmpty()
					&& currentLayer == null) {
				return mergedLayerList;
			}
			List<Layer> visibleLayers = new ArrayList<>();
			visibleLayers.addAll(mergedLayerList);
			Collections.reverse(visibleCommittedTransactions);
			visibleCommittedTransactions.stream()
					.map(nonMergedTransactionLayers::get)
					.forEach(visibleLayers::add);
			if (currentLayer != null) {
				visibleLayers.add(currentLayer);
			}
			/*
			 * et voila!
			 */
			return visibleLayers;
		}
	}

	/*
	 * Does not allow modification of any layers _visible to this tx_ during
	 * iteration.
	 */
	class TransactionalEntrySet extends AbstractSet<Entry<K, V>> {
		private List<Layer> visibleLayers;

		private int size = -1;

		private Transaction transaction;

		public TransactionalEntrySet() {
			visibleLayers = visibleLayers();
			transaction = Transaction.current();
			if (transaction.isEnded()) {
				throw new MvccException(
						"Creating tx entry set outside of a transaction");
			}
		}

		private int calculateSize() {
			return visibleLayers.stream()
					.collect(Collectors.summingInt(layer -> layer.added
							- (layer.hasRemoved() ? layer.removed.size() : 0)));
		}

		@Override
		public void clear() {
			super.clear();
		}

		Iterator<Entry<K, V>> comparatorIterator() {
			List<Iterator<Entry<K, V>>> iterators = visibleLayers.stream()
					.map(Layer::modifiedEntrySetIterator)
					.collect(Collectors.toList());
			Iterator<Entry<K, V>>[] iteratorArray = (Iterator<Entry<K, V>>[]) iterators
					.toArray(new Iterator[iterators.size()]);
			MultiIterator<Entry<K, V>> layerIterator = new MultiIterator<Entry<K, V>>(
					false,
					comparator == null ? null : new Comparator<Entry<K, V>>() {
						@Override
						public int compare(Entry<K, V> o1, Entry<K, V> o2) {
							return comparator.compare(o1.getKey(), o2.getKey());
						}
					}, iteratorArray);
			Predicate<Entry<K, V>> notVisibleFilter = e -> {
				int idx = layerIterator.getCurrentIteratorIndex();
				if (visibleLayers.get(idx).wasRemoved(e.getKey())) {
					return false;
				}
				idx++;// check if modified in any subsequent layers
				for (; idx < visibleLayers.size(); idx++) {
					if (visibleLayers.get(idx)
							.wasModifiedOrRemoved(e.getKey())) {
						// will be returned (or not, if removed in a subsequent
						// layer) by the subsequent layer iterator
						return false;
					}
				}
				return true;
			};
			return new CrossTxItr(
					new FilteringIterator<>(layerIterator, notVisibleFilter));
		}

		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}

		@Override
		/*
		 * FIXME - mvcc.jobs.1a - in fact overly complex
		 * 
		 * Instead, traverse from the top down, retaining a collection of
		 * visited/removed - except for the bottom layer
		 * 
		 * Because the final (bottom) layer is almost sure to be >> the size of
		 * the others, memory effects will be limited and won't require multiple
		 * traversals per iteration
		 */
		public Iterator<Entry<K, V>> iterator() {
			return comparator == null ? new CrossTxItr(new LayerIterator())
					: comparatorIterator();
		}

		@Override
		//
		public int size() {
			if (size == -1) {
				size = calculateSize();
			}
			return size;
		}

		private class CrossTxItr implements Iterator<Entry<K, V>> {
			private Iterator<Entry<K, V>> delegate;

			public CrossTxItr(Iterator<Entry<K, V>> delegate) {
				this.delegate = delegate;
			}

			@Override
			public boolean hasNext() {
				return delegate.hasNext();
			}

			@Override
			public Entry<K, V> next() {
				Entry<K, V> next = delegate.next();
				if (transaction.isEnded()) {
					// this case is generally a long-running job traversing
					// the
					// graph. use the current tx state of the map to filter
					// (less optimal but see the bit about 'long running')
					// we're guaranteed to not see spurious entities -
					// although
					// of course we'll miss those in the later txs
					// note that this only works for idMaps where we're
					// guaranteed unchanging value objects - other maps
					// (projections/lookups) shouldn't be accessed across tx
					// boundaries
					//
					// Because changing the size of the iterator causes
					// problems (e.g. in stream iteration), instead return
					// null -- calling code must be prepared to filter
					// nulls.
					if (!immutableValues) {
						throw new MvccException(
								"Accessing non-immutable value iterator across tx boundary");
					}
					if (!TransactionalMapOld.this.containsKey(next.getKey())) {
						return new Entry<K, V>() {
							@Override
							public K getKey() {
								return next.getKey();
							}

							@Override
							public V getValue() {
								return null;
							}

							@Override
							public V setValue(V value) {
								throw new UnsupportedOperationException();
							}
						};
					}
				}
				return next;
			}
		}

		private class LayerIterator implements Iterator<Entry<K, V>> {
			Set<K> visitedOrRemoved = new ObjectOpenHashSet<>();

			private FilteringIterator<Entry<K, V>> filteringIterator;

			public LayerIterator() {
				List<Iterator<Entry<K, V>>> iterators = visibleLayers.stream()
						.map(Layer::modifiedEntrySetIterator)
						.collect(Collectors.toList());
				Collections.reverse(iterators);
				Iterator<Entry<K, V>>[] iteratorArray = (Iterator<Entry<K, V>>[]) iterators
						.toArray(new Iterator[iterators.size()]);
				MultiIterator<Entry<K, V>> layerIterator = new MultiIterator<Entry<K, V>>(
						false, null, iteratorArray) {
					@Override
					protected void onBeforeIteratorIndexChange(
							int currentIteratorIndex) {
						// our iterators are reversed, so reverse index access
						// to visibleLayers
						int visibleLayerIndex = iterators.size()
								- currentIteratorIndex - 1;
						if (visibleLayerIndex == 0) {
							// the lowest layer, no need to mark
							return;
						}
						visitedOrRemoved.addAll(
								visibleLayers.get(visibleLayerIndex).modified
										.keySet());
						if (visibleLayers
								.get(visibleLayerIndex).removed != null) {
							visitedOrRemoved.addAll(
									visibleLayers.get(visibleLayerIndex).removed
											.keySet());
						}
					}
				};
				Predicate<Entry<K, V>> notVisitedOrRemovedFilter = e -> {
					return !visitedOrRemoved.contains(e.getKey());
				};
				filteringIterator = new FilteringIterator<>(layerIterator,
						notVisitedOrRemovedFilter);
			}

			@Override
			public boolean hasNext() {
				return filteringIterator.hasNext();
			}

			@Override
			public Entry<K, V> next() {
				return filteringIterator.next();
			}
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
	}
}
