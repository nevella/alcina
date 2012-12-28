package cc.alcina.framework.gwt.persistence.client;

import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.IntPair;

public class PropertyStore {
	protected PropertyStore() {
		super();
	}

	private static PropertyStore theInstance;

	protected ObjectStore objectStore;

	public static PropertyStore get() {
		if (theInstance == null) {
			theInstance = new PropertyStore();
		}
		return theInstance;
	}
	/**
	 * 
	 */
	public static PropertyStore createNonStandardPropertyStore(ObjectStore delegate) {
		PropertyStore store = new PropertyStore();
		store.registerDelegate(delegate);
		return store;
	}
	public void registerDelegate(ObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void put(String key, String value,
			PersistenceCallback<Integer> idCallback) {
		this.objectStore.put(key, value, idCallback);
	}

	public void get(String key, PersistenceCallback<String> valueCallback) {
		this.objectStore.get(key, valueCallback);
	}

	public void remove(String key,
			PersistenceCallback<Integer> completedCallback) {
		this.objectStore.remove(key, completedCallback);
	}

	public void getKeysPrefixedBy(String keyPrefix,
			PersistenceCallback<List<String>> completedCallback) {
		this.objectStore.getKeysPrefixedBy(keyPrefix, completedCallback);
	}

	public ObjectStore getObjectStore() {
		return this.objectStore;
	}
}
