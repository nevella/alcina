package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MappingIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MultiIterator;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction.TransactionComparator;
import it.unimi.dsi.fastutil.longs.Long2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

/*
 * Synchronization:
 * 
 * 'this' acts as a lock on modification of the layers field (since layers may be null)
 * 
 * This class is intended for transaction-aware indices and lookups
 * 
 * The map is composed of a base layer, a list of (transactional or merged-transactional) layers corresponding to deltas
 *  at the time of the transaction(s) (note that these layers are visible to all current transactions) 
 *  and a third grouping of transactional layers 
 *   ('layers') whose resolution is transaction-depdendent.
 *   
 *    Operations are resolved to return the most recent value
 *   for the combined sequence of layers, from (per-transaction) youngest-to-oldest
 *   
 */
public class TransactionalMap<K, V> extends AbstractMap<K, V> {
	private Layer base;

	private ConcurrentSkipListMap<Transaction, Layer> layers;

	private List<Layer> baseLayerList;

	private Class<K> keyClass;

	private Class<V> valueClass;

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
		Objects.requireNonNull(key);
		if (layers == null) {
			return base.get(key);
		}
		List<TransactionalMap<K, V>.Layer> visibleLayers = visibleLayers();
		for (int idx = visibleLayers.size() - 1; idx > 0; idx--) {
			Layer layer = visibleLayers.get(idx);
			if (layer.wasModifiedOrRemoved(key)) {
				return layer.get(key);
			}
			if (layer.wasRemoved(key)) {
				return null;
			}
		}
		return null;
	}

	@Override
	public Set<K> keySet() {
		return new KeySet();
	}

	@Override
	public V put(K key, V value) {
		/*
		 * May need to relax this (projections with null keys) - or have null
		 * markers
		 */
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		V existing = get(key);
		Layer layer = ensureLayer();
		layer.put(key, value, existing);
		return existing;
	}

	@Override
	public V remove(Object key) {
		Objects.requireNonNull(key);
		V existing = get(key);
		Layer layer = ensureLayer();
		layer.remove((K) key);
		return existing;
	}

	@Override
	public Collection<V> values() {
		if (HasIdAndLocalId.class.isAssignableFrom(valueClass)) {
			return new ValuesSet();
		} else {
			return new ValuesCollection();
		}
	}

	private TransactionalMap<K, V>.Layer ensureLayer() {
		// TEST
		Transaction transaction = Transaction.current();
		if (transaction.isBaseTransaction()) {
			if (base == null) {
				synchronized (this) {
					if (base == null) {
						base = new Layer(transaction);
					}
				}
			}
			return base;
		}
		if (layers == null || !layers.containsKey(transaction)) {
			// double-checked
			synchronized (this) {
				if (layers == null || !layers.containsKey(transaction)) {
					if (layers == null) {
						layers = new ConcurrentSkipListMap<>(
								new TransactionComparator());
					}
					layers.put(transaction, new Layer(transaction));
				}
			}
		}
		return layers.get(transaction);
	}

	private List<Layer> visibleLayers() {
		if (layers == null) {
			return baseLayerList;
		} else {
			List<Layer> visibleLayers = new ArrayList<>();
			visibleLayers.add(base);
			Transaction transaction = Transaction.current();
			/*
			 * for the moment, assume visible tx size not >> layer size
			 */
			for (Transaction committed : transaction.committedTransactions
					.values()) {
				TransactionalMap<K, V>.Layer layer = layers.get(committed);
				if (layer != null) {
					visibleLayers.add(layer);
				}
			}
			return visibleLayers;
		}
	}

	protected <K1, V1> Map<K1, V1> createNonSynchronizedMap(Class<K1> keyClass,
			Class<V1> valueClass) {
		if (keyClass == Long.class) {
			if (valueClass == Boolean.class) {
				return (Map<K1, V1>) new Long2BooleanLinkedOpenHashMap();
			} else {
				return (Map<K1, V1>) new Long2ObjectLinkedOpenHashMap<>();
			}
		} else {
			// FIXME - some more optimisations would be great
			return (Map<K1, V1>) new Object2ObjectLinkedOpenHashMap<>();
		}
	}

	private class KeySet extends AbstractSet<K> {
		private Set<Entry<K, V>> entrySet;

		public KeySet() {
			this.entrySet = entrySet();
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
		Map<K, Boolean> removed = createNonSynchronizedMap(keyClass,
				Boolean.class);

		Map<K, V> modified = createNonSynchronizedMap(keyClass, valueClass);

		Transaction transaction;

		public Layer(Transaction transaction) {
			this.transaction = transaction;
		}

		public V get(Object key) {
			return modified.get(key);
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

		public boolean wasModifiedOrRemoved(Object key) {
			return removed.containsKey(key) || modified.containsKey(key);
		}

		public boolean wasRemoved(Object key) {
			return removed.containsKey(key);
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
		public Iterator<Entry<K, V>> iterator() {
			List<Iterator<Entry<K, V>>> iterators = visibleLayers.stream()
					.map(Layer::modifiedEntrySetIterator)
					.collect(Collectors.toList());
			Iterator<Entry<K, V>>[] iteratorArray = (Iterator<Entry<K, V>>[]) iterators
					.toArray(new Iterator[iterators.size()]);
			MultiIterator<Entry<K, V>> layerIterator = new MultiIterator<Entry<K, V>>(
					false, iteratorArray);
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
