package cc.alcina.framework.gwt.persistence.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class PropertyStore {
	protected PropertyStore() {
		super();
	}

	private static PropertyStore theInstance;

	protected PersistenceObjectStore objectStore;

	public static PropertyStore get() {
		if (theInstance == null) {
			theInstance = new PropertyStore();
		}
		return theInstance;
	}
	/**
	 * 
	 */
	public static PropertyStore createNonStandardPropertyStore(PersistenceObjectStore delegate) {
		PropertyStore store = new PropertyStore();
		store.registerDelegate(delegate);
		return store;
	}
	public void registerDelegate(PersistenceObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void put(String key, String value,
			AsyncCallback<Integer> idCallback) {
		this.objectStore.put(key, value, idCallback);
	}

	public void get(String key, AsyncCallback<String> valueCallback) {
		this.objectStore.get(key, valueCallback);
	}

	public void remove(String key,
			AsyncCallback<Integer> completedCallback) {
		this.objectStore.remove(key, completedCallback);
	}

	public void getKeysPrefixedBy(String keyPrefix,
			AsyncCallback<List<String>> completedCallback) {
		this.objectStore.getKeysPrefixedBy(keyPrefix, completedCallback);
	}

	public PersistenceObjectStore getObjectStore() {
		return this.objectStore;
	}
}
