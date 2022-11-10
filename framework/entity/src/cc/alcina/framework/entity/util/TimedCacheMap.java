package cc.alcina.framework.entity.util;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Map implementation that removes entries after they reach a certain age
 * </p>
 * <p>
 * The eviction task only runs while there are entries in the map, and is
 * stopped when the cache is emptied.
 * </p>
 * <p>
 * The underlying map is a ConcurrentHashMap, so this is thread-safe
 * (relatively)
 * </p>
 */
public class TimedCacheMap<K, V> implements Map<K, V> {
	private Logger LOGGER = LoggerFactory.getLogger(TimedCacheMap.class);

	private ConcurrentHashMap<K, TimedCacheValue<V>> delegate;

	private long expiry;

	private long evictionInterval;

	private Timer evictionTimer;

	private TimerTask evictionTimerTask;

	/**
	 * Instantiate a new TimedCacheMap with given expiry
	 * 
	 * @param expiry
	 *            Milliseconds until an entry is expired
	 */
	public TimedCacheMap(long expiry) {
		this(expiry, expiry);
	}

	/**
	 * Instantiate a new TimedCacheMap with given expiry and customer eviction
	 * timer interval
	 * 
	 * @param expiry
	 *            Milliseconds until an entry is expired
	 * @param evictionInterval
	 *            Milliseconds before entries are checked and evicted if expired
	 */
	public TimedCacheMap(long expiry, long evictionInterval) {
		this.delegate = new ConcurrentHashMap<>();
		this.expiry = expiry;
		this.evictionInterval = evictionInterval;
		this.evictionTimer = new Timer();
	}

	@Override
	public V put(K key, V value) {
		// Wrap value in a TimedCacheValue to track time of entry and insert
		TimedCacheValue<V> old = delegate.put(key,
				new TimedCacheValue<V>(value));
		// Ensure the eviction timer is running
		ensureTimerRunning();
		// Unwrap and return old value
		return old != null ? old.value : null;
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		/**
		 * Implementation effectively copied from
		 * ConcurrentHashMap.containsValue() Only change is that it checks the
		 * unwrapped value rather than the wrapped
		 */
		if (value == null) {
			throw new NullPointerException();
		}
		for (Map.Entry<K, TimedCacheValue<V>> entry : delegate.entrySet()) {
			TimedCacheValue<V> tce = entry.getValue();
			if (tce != null && (tce.value == value
					|| (tce.value != null && value.equals(tce.value)))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V get(Object key) {
		// Get wrapped value from the delegate map
		TimedCacheValue<V> tce = delegate.get(key);
		// Unwrap and return
		return tce != null ? tce.value : null;
	}

	@Override
	public V remove(Object key) {
		// Remove from delegate map
		TimedCacheValue<V> tce = delegate.remove(key);
		// If the map is now empty, cancel the eviction timer
		if (isEmpty()) {
			ensureTimerStopped();
		}
		// Unwrap and return
		return tce != null ? tce.value : null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		// Call put with all elements of the set
		for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		// Clear the delegate map
		delegate.clear();
		// Cancel the eviction timer
		ensureTimerStopped();
	}

	/**
	 * Dereference the underlying map and cancel the internal eviction timer,
	 * releasing the held timer thread
	 */
	public void destroy() {
		delegate.clear();
		delegate = null;
		evictionTimer.cancel();
	}

	@Override
	public Set<K> keySet() {
		return delegate.keySet();
	}

	@Override
	public Collection<V> values() {
		// Map all delegate map values to their unwrapped form
		return delegate.values().stream().map(v -> v.value)
				.collect(Collectors.toList());
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		// Map all delegate map entries to TimedCacheEntry's with the unwrapped value
		return delegate.entrySet().stream()
				.map(es -> new TimedCacheEntry<K, V>(es))
				.collect(Collectors.toSet());
	}

	/**
	 * Check all delegate map entries for expiration, and remove if expired
	 */
	private void checkEntries() {
		// Iterate over all keys
		for (K key : keySet()) {
			// Check the creation time of each entry
			TimedCacheValue<V> tcv = delegate.get(key);
			if ((new Date().getTime() - tcv.created.getTime()) > expiry) {
				// If the creation time is past expected expiry, evict the entry
				remove(key);
				LOGGER.debug("Evicted entry: {map={}, key={}}", this, key);
			}
		}
	}

	/**
	 * Ensure the eviction timer task is running
	 */
	private void ensureTimerRunning() {
		synchronized (this) {
			if (evictionTimerTask == null) {
				evictionTimerTask = new TimerTask() {
					@Override
					public void run() {
						checkEntries();
					}
				};
				evictionTimer.scheduleAtFixedRate(evictionTimerTask,
						evictionInterval, evictionInterval);
				LOGGER.debug("Timed cache eviction timer started: {map={}}", this);
			}
		}
	}

	/**
	 * Ensure the eviction timer task is stopped
	 */
	private void ensureTimerStopped() {
		synchronized (this) {
			if (evictionTimerTask != null) {
				evictionTimerTask.cancel();
				evictionTimerTask = null;
				LOGGER.debug("Timed cache eviction timer stopped: {map={}}", this);
			}
		}
	}

	/**
	 * Wrapped cache values with the time of creation
	 */
	static class TimedCacheValue<V> {
		Date created;

		V value;

		TimedCacheValue(V value) {
			this.created = new Date();
			this.value = value;
		}
	}

	/**
	 * Wrapped cache entries which unwrap values as needed
	 */
	static class TimedCacheEntry<K, V> implements Map.Entry<K, V> {
		Map.Entry<K, TimedCacheValue<V>> delegateEntry;

		TimedCacheEntry(Map.Entry<K, TimedCacheValue<V>> delegateEntry) {
			this.delegateEntry = delegateEntry;
		}

		@Override
		public K getKey() {
			return delegateEntry.getKey();
		}

		@Override
		public V getValue() {
			TimedCacheValue<V> tcv = delegateEntry.getValue();
			return tcv != null ? tcv.value : null;
		}

		@Override
		public V setValue(V value) {
			// Create a new TimedCacheValue and set it on the delegate entry
			TimedCacheValue<V> old = delegateEntry.setValue(new TimedCacheValue<V>(value));
			// Return the old value unwrapped if present
			return old != null ? old.value : null;
		}
	}
}
