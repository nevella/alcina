package cc.alcina.framework.gwt.persistence.client;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.StringMap;

public interface PersistenceObjectStore {
	public abstract void getRange(int fromId, int toId,
			AsyncCallback<Map<Integer, String>> valueCallback);

	public abstract void add(String key, String value,
			AsyncCallback<Integer> idCallback);

	public abstract void put(String key, String value,
			AsyncCallback<Integer> idCallback);

	public abstract void put(int id, String value,
			AsyncCallback<Void> completedCallback);

	public abstract void put(StringMap kvs, AsyncCallback idCallback);

	public abstract void get(String key, AsyncCallback<String> valueCallback);

	public abstract void get(List<String> keys,
			AsyncCallback<StringMap> valueCallback);

	public abstract void remove(String key, AsyncCallback<Integer> valueCallback);

	public abstract void getKeysPrefixedBy(String keyPrefix,
			AsyncCallback<List<String>> completedCallback);

	void getIdRange(AsyncCallback<IntPair> completedCallback);

	void removeIdRange(IntPair range, AsyncCallback<Void> completedCallback);

	void drop(AsyncCallback<Void> AsyncCallback);

	public abstract void remove(List<String> keys,
			AsyncCallback completedCallback);

	void clear(AsyncCallback<Void> AsyncCallback);

	public abstract String getTableName();
}