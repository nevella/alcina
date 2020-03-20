package cc.alcina.framework.entity.entityaccess.cache.mvcc;

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
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction.TransactionComparator;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Vacuum.Vacuumable;
import cc.alcina.framework.entity.projection.GraphProjection;
import it.unimi.dsi.fastutil.longs.Long2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

/*
 * 
 * This class is intended for transaction-aware indices and lookups. 
 * A 'layer' records deltas to the map state by recording modifications and removals in separate, non-locked submaps.
 * 
 * The map is composed of a base layer, a list of visible-to-all-transactions merged layers 
 * and a list of current transaction layers whose visibility is transaction-dependent.
 * 
 * The merged layers are generated during vacuum, and are 
 *  intended to avoid runaway map layers for heavily written maps by using the levelled compaction algorithm 
 *   similar to that used in Cassandra and Chromium leveldb compactions.
 *  
 *  
 *  Note that vacuum is probably the most interesting part of this class - it's like sliding plates
 *   (B=merged A1, A2) under other plates (A1, A2) - it would be nice 
 *   to formally prove that the combination (B,A2) === (B) === (B,A1,A2) (in layer order)
 * 
 *   
 *   Operations are resolved to return the most recent value
 *   for the combined sequence of layers, from (per-transaction) youngest-to-oldest
 *   
 *   This class allows null keys and values
 *   
 *   Synchronization:
 * 
 * 'this' acts as a lock on modification of the layers field (since layers may be null)
 * 
 * 
 *   TODO - baseLayerList can be implemented by this class (so avoid having an extra list per map) 
 *   
 */
public class TransactionalMap<K, V> extends AbstractMap<K, V>
		implements Vacuumable {
	private Layer base;

	private volatile ConcurrentSkipListMap<Transaction, Layer> transactionLayers;

	private List<Layer> baseLayerList;

	private volatile List<Layer> mergedLayerList;

	protected Class<K> keyClass;

	private Class<V> valueClass;

	Comparator<K> comparator;

	public TransactionalMap(Class<K> keyClass, Class<V> valueClass) {
		this.keyClass = keyClass;
		this.valueClass = valueClass;
		base = new Layer(Transaction.current());
		baseLayerList = Collections.singletonList(base);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new TransactionalEntrySet();
	}

	@Override
	public V get(Object key) {
		if (transactionLayers == null) {
			return base.get(key);
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
		Preconditions.checkArgument(
				value == null || valueClass.isAssignableFrom(value.getClass()));
		V existing = get(key);
		Layer layer = ensureLayer();
		layer.put(key, value, existing);
		return existing;
	}

	@Override
	public V remove(Object key) {
		V existing = get(key);
		Layer layer = ensureLayer();
		layer.remove((K) key);
		return existing;
	}

	@Override
	public String toString() {
		return Ax.format("tx.map: %s=>%s : %s layers : %s objects in this tx",
				keyClass.getSimpleName(), valueClass.getSimpleName(),
				transactionLayers == null ? 0 : transactionLayers.size(),
				entrySet().size());
	}

	@Override
	public void vacuum(Transaction transaction) {
		if (transaction.getPhase() != TransactionPhase.TO_DOMAIN_COMMITTED) {
			transactionLayers.remove(transaction);
			return;
		}
		Layer layer = transactionLayers.get(transaction);
		List<Layer> mergedReplace = new ArrayList<>(mergedLayerList);
		mergedReplace.add(layer);
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
					Layer merged = layer0.merge(layer1);
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
		mergedLayerList = mergedReplace;
		transactionLayers.remove(transaction);
	}

	@Override
	public Collection<V> values() {
		if (Entity.class.isAssignableFrom(valueClass)) {
			return new ValuesSet();
		} else {
			return new ValuesCollection();
		}
	}

	private TransactionalMap<K, V>.Layer ensureLayer() {
		Transaction transaction = Transaction.current();
		if (transaction.isBaseTransaction()) {
			return base;
		}
		if (transactionLayers == null
				|| !transactionLayers.containsKey(transaction)) {
			// double-checked
			synchronized (this) {
				if (transactionLayers == null
						|| !transactionLayers.containsKey(transaction)) {
					if (transactionLayers == null) {
						transactionLayers = new ConcurrentSkipListMap<>(
								new TransactionComparator());
						mergedLayerList = new ArrayList<>();
					}
					transactionLayers.put(transaction, new Layer(transaction));
					Transactions.get().onAddedVacuumable(this);
				}
			}
		}
		return transactionLayers.get(transaction);
	}

	private List<Layer> visibleLayers() {
		if (transactionLayers == null) {
			return baseLayerList;
		} else {
			Transaction transaction = Transaction.current();
			List<Layer> visibleLayers = new ArrayList<>();
			List<Layer> transactionLayersSnapshot = new ArrayList<>();
			visibleLayers.add(base);
			/*
			 * To avoid synchronization, copy visible layers *before* merged
			 * layers (because vacuum modifies visible layers *after* merged
			 * layers)
			 */
			for (Transaction committed : transaction.committedTransactions
					.values()) {
				Layer layer = transactionLayers.get(committed);
				if (layer != null) {
					transactionLayersSnapshot.add(layer);
				}
			}
			/*
			 * Note that merged layer list is swapped during vacuum, rather than
			 * modified (its length is logarithmic wrt element size) - so we can
			 * add without a synchronization risk.
			 */
			visibleLayers.addAll(mergedLayerList);
			/*
			 * now add our snapshot
			 */
			visibleLayers.addAll(transactionLayersSnapshot);
			Layer currentLayer = transactionLayers.get(Transaction.current());
			if (currentLayer != null) {
				visibleLayers.add(currentLayer);
			}
			/*
			 * et voila!
			 */
			return visibleLayers;
		}
	}

	protected <V1> Map<K, V1> createNonSynchronizedMap(Class<V1> valueClass) {
		if (keyClass == Long.class) {
			if (valueClass == Boolean.class) {
				return (Map<K, V1>) new Long2BooleanLinkedOpenHashMap();
			} else {
				return (Map<K, V1>) new Long2ObjectLinkedOpenHashMap<>();
			}
		} else {
			// FIXME - some more optimisations would be great
			return (Map<K, V1>) new Object2ObjectLinkedOpenHashMap<>();
		}
	}

	private class KeySet extends AbstractSet<K> {
		private Set<Entry<K, V>> entrySet;

		public KeySet() {
			this.entrySet = entrySet();
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

	class Layer {
		int added;

		/*
		 * FIXME - these should be specialised. Removed should be lazy
		 */
		Map<K, Boolean> removed = createNonSynchronizedMap(Boolean.class);

		Map<K, V> modified = createNonSynchronizedMap(valueClass);

		Transaction transaction;

		public Layer(Transaction transaction) {
			this.transaction = transaction;
		}

		public V get(Object key) {
			return modified.get(key);
		}

		/*
		 * returns a new non-transaction layer containing the union of these two
		 */
		public Layer merge(Layer higherLayer) {
			Layer merged = new Layer(null);
			merged.removed.putAll(removed);
			merged.modified.putAll(modified);
			merged.removed.keySet().removeAll(higherLayer.modified.keySet());
			merged.modified.keySet().removeAll(higherLayer.removed.keySet());
			merged.removed.putAll(higherLayer.removed);
			merged.modified.putAll(higherLayer.modified);
			return merged;
		}

		public void put(K key, V value, V existing) {
			modified.put(key, value);
			if (existing == null) {
				added++;
				removed.remove(key);
			}
		}

		public void remove(K key) {
			removed.put(key, Boolean.TRUE);
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
			return removed.containsKey(key) || modified.containsKey(key);
		}

		public boolean wasRemoved(Object key) {
			return removed.containsKey(key);
		}

		int combinedMapSize() {
			return removed.size() + modified.size();
		}

		Iterator<Entry<K, V>> modifiedEntrySetIterator() {
			Iterator<Entry<K, V>> iterator = modified.entrySet().iterator();
			return iterator;
		}
	}

	class TransactionalEntrySet extends AbstractSet<Entry<K, V>> {
		private List<Layer> visibleLayers;

		public TransactionalEntrySet() {
			visibleLayers = visibleLayers();
		}

		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
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
			Predicate<Entry<K, V>> mapping = e -> {
				int idx = layerIterator.getCurrentIteratorIndex();
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
			FilteringIterator<Entry<K, V>> filteringIterator = new FilteringIterator<>(
					layerIterator, mapping);
			return filteringIterator;
		}

		@Override
		public int size() {
			return visibleLayers.stream().collect(Collectors
					.summingInt(layer -> layer.added - layer.removed.size()));
		}
	}
}
