package cc.alcina.framework.gwt.persistence.client;

import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.IntPair;

public interface ObjectStore {

	public abstract void getRange( int fromId,  int toId,  PersistenceCallback<Map<Integer, String>> valueCallback);

	public abstract void add(String key, String value, PersistenceCallback<Integer> idCallback);

	public abstract void put(String key, String value, PersistenceCallback<Integer> idCallback);

	public abstract void get( String key,  PersistenceCallback<String> valueCallback);
	public abstract void remove( String key,  PersistenceCallback<Integer> valueCallback);

	public abstract void getKeysPrefixedBy(String keyPrefix,
			PersistenceCallback<List<String>> completedCallback);

	void getIdRange(PersistenceCallback<IntPair> completedCallback);
	void removeIdRange(IntPair range,PersistenceCallback<Void> completedCallback);

	void drop(PersistenceCallback<Void> persistenceCallback);

}