package cc.alcina.framework.gwt.persistence.client;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.CollectionFilters.PrefixedFilter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.util.AsyncCallbackNull;
import cc.alcina.framework.gwt.client.util.DiscardInfoWrappingCallback;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class ObjectStoreMemoryImpl implements PersistenceObjectStore,
		SyncObjectStore {
	private TreeMap<Integer, String> values = new TreeMap<Integer, String>();

	private Multimap<String, List<Integer>> reverseKeys = new Multimap<String, List<Integer>>();

	private TreeMap<Integer, String> keys = new TreeMap<Integer, String>();

	int idCtr = 0;

	@Override
	public void add(String key, String value, AsyncCallback<Integer> idCallback) {
		int id = nextId();
		_put(key, value, idCallback, id);
	}

	@Override
	public void drop(AsyncCallback<Void> AsyncCallback) {
		values.clear();
		reverseKeys.clear();
		keys.clear();
	}

	@Override
	public String dumpValuesAsStringList() {
		return CommonUtils.join(values.values(), "\n");
	}

	@Override
	public void get(String key, AsyncCallback<String> valueCallback) {
		String value = valueForKey(key);
		valueCallback.onSuccess(value);
	}

	protected String valueForKey(String key) {
		int id = getFirstId(key);
		String value = id == 0 ? null : values.get(id);
		return value;
	}

	@Override
	public void getIdRange(AsyncCallback<IntPair> completedCallback) {
		IntPair range = keys.isEmpty() ? new IntPair() : new IntPair(
				keys.firstKey(), keys.lastKey());
		completedCallback.onSuccess(range);
	}

	@Override
	public void getKeysPrefixedBy(String keyPrefix,
			AsyncCallback<List<String>> completedCallback) {
		List<String> prefixed = CollectionFilters.filter(reverseKeys.keySet(),
				new PrefixedFilter(keyPrefix));
		completedCallback.onSuccess(prefixed);
	}

	@Override
	public void getRange(int fromId, int toId,
			AsyncCallback<Map<Integer, String>> valueCallback) {
		valueCallback.onSuccess(values.subMap(fromId, toId + 1));
	}

	@Override
	public void put(int id, String value, AsyncCallback<Void> idCallback) {
		_put(keys.get(id), value, new DiscardInfoWrappingCallback<Integer>(
				idCallback), id);
	}

	@Override
	public void put(String key, String value, AsyncCallback<Integer> idCallback) {
		int id = getFirstId(key);
		id = id == 0 ? nextId() : id;
		_put(key, value, idCallback, id);
	}

	@Override
	public void remove(String key, AsyncCallback<Integer> valueCallback) {
		int id = getFirstId(key);
		if (id != 0) {
			remove(id);
		}
		valueCallback.onSuccess(id);
	}

	@Override
	public void removeIdRange(IntPair range,
			AsyncCallback<Void> completedCallback) {
		for (int id = range.i1; id <= range.i2; id++) {
			remove(id);
		}
		completedCallback.onSuccess(null);
	}

	private int getFirstId(String key) {
		List<Integer> ids = reverseKeys.getAndEnsure(key);
		int id = 0;
		if (ids.size() > 0) {
			id = ids.get(0);
		}
		return id;
	}

	@SuppressWarnings("unused")
	private synchronized int getId() {
		return idCtr;
	}

	private synchronized int nextId() {
		return ++idCtr;
	}

	protected void _put(String key, String value,
			AsyncCallback<Integer> idCallback, int id) {
		keys.put(id, key);
		values.put(id, value);
		reverseKeys.remove(key, id);
		reverseKeys.add(key, id);
		idCallback.onSuccess(id);
	}

	protected void remove(int id) {
		String kv = keys.remove(id);
		values.remove(id);
		if (kv != null) {
			reverseKeys.remove(kv, id);
		}
	}

	@Override
	public void put(StringMap kvs, AsyncCallback callback) {
		for (Entry<String, String> e : kvs.entrySet()) {
			put(e.getKey(), e.getValue(), new AsyncCallbackNull());
		}
		callback.onSuccess(null);
	}

	@Override
	public void get(List<String> keys, AsyncCallback<StringMap> valueCallback) {
		StringMap values = new StringMap();
		for (String key : keys) {
			values.put(key, valueForKey(key));
		}
		valueCallback.onSuccess(values);
	}

	@Override
	public void remove(List<String> keys, AsyncCallback completedCallback) {
		for (String key : keys) {
			remove(key, new AsyncCallbackNull());
		}
		completedCallback.onSuccess(null);
	}

	@Override
	public void clear(AsyncCallback<Void> AsyncCallback) {
		values.clear();
		keys.clear();
		reverseKeys.clear();
		idCtr = 0;
	}

	@Override
	public String getTableName() {
		return null;
	}
}
