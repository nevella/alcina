package cc.alcina.framework.gwt.persistence.client;

import java.util.Map;

public interface ObjectStore {

	public abstract void getRange( int fromId,  int toId,  PersistenceCallback<Map<Integer, String>> valueCallback);

	public abstract void add(String key, String value, PersistenceCallback<Integer> idCallback);

	public abstract void put(String key, String value, PersistenceCallback<Integer> idCallback);

	public abstract void get( String key,  PersistenceCallback<String> valueCallback);
}