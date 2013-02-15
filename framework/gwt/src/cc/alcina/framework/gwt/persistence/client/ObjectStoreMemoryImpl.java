package cc.alcina.framework.gwt.persistence.client;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.CollectionFilters.PrefixedFilter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multimap;

public class ObjectStoreMemoryImpl implements ObjectStore, SyncObjectStore {
	private TreeMap<Integer, String> values = new TreeMap<Integer, String>();

	private Multimap<String, List<Integer>> reverseKeys = new Multimap<String, List<Integer>>();

	private TreeMap<Integer, String> keys = new TreeMap<Integer, String>();

	int idCtr = 0;

	@Override
	public void getRange(int fromId, int toId,
			PersistenceCallback<Map<Integer, String>> valueCallback) {
		valueCallback.onSuccess(values.subMap(fromId, toId+1));
	}

	@Override
	public void add(String key, String value,
			PersistenceCallback<Integer> idCallback) {
		int id = nextId();
		_put(key, value, idCallback, id);
	}

	protected void _put(String key, String value,
			PersistenceCallback<Integer> idCallback, int id) {
		keys.put(id, key);
		values.put(id, value);
		reverseKeys.remove(key, id);
		reverseKeys.add(key, id);
		idCallback.onSuccess(id);
	}

	private synchronized int nextId() {
		return ++idCtr;
	}

	@SuppressWarnings("unused")
	private synchronized int getId() {
		return idCtr;
	}

	@Override
	public void put(String key, String value,
			PersistenceCallback<Integer> idCallback) {
		int id = getFirstId(key);
		id = id == 0 ? nextId() : id;
		_put(key, value, idCallback, id);
	}

	private int getFirstId(String key) {
		List<Integer> ids = reverseKeys.getAndEnsure(key);
		int id = 0;
		if (ids.size() > 0) {
			id = ids.get(0);
		}
		return id;
	}

	@Override
	public void get(String key, PersistenceCallback<String> valueCallback) {
		int id = getFirstId(key);
		String value = id == 0 ? null : values.get(id);
		valueCallback.onSuccess(value);
	}

	@Override
	public void remove(String key, PersistenceCallback<Integer> valueCallback) {
		int id = getFirstId(key);
		if (id != 0) {
			remove(id);
		}
		valueCallback.onSuccess(id);
	}

	protected void remove(int id) {
		String kv = keys.remove(id);
		values.remove(id);
		if (kv != null) {
			reverseKeys.remove(kv, id);
		}
	}

	@Override
	public void getKeysPrefixedBy(String keyPrefix,
			PersistenceCallback<List<String>> completedCallback) {
		List<String> prefixed = CollectionFilters.filter(reverseKeys.keySet(),
				new PrefixedFilter(keyPrefix));
		completedCallback.onSuccess(prefixed);
	}

	@Override
	public void getIdRange(PersistenceCallback<IntPair> completedCallback) {
		IntPair range = keys.isEmpty() ? new IntPair() : new IntPair(
				keys.firstKey(), keys.lastKey());
		completedCallback.onSuccess(range);
	}

	@Override
	public void removeIdRange(IntPair range,
			PersistenceCallback<Void> completedCallback) {
		for (int id = range.i1; id <= range.i2; id++) {
			remove(id);
		}
		completedCallback.onSuccess(null);
	}

	@Override
	public void drop(PersistenceCallback<Void> persistenceCallback) {
		values.clear();
		reverseKeys.clear();
		keys.clear();
	}

	@Override
	public String dumpValuesAsStringList() {
		return CommonUtils.join(values.values(), "\n");
	}
}
