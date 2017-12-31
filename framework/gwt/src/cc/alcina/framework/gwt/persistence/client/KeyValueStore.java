package cc.alcina.framework.gwt.persistence.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class KeyValueStore {
	public static KeyValueStore
			createNonStandardKeyValueStore(PersistenceObjectStore delegate) {
		KeyValueStore store = new KeyValueStore();
		store.registerDelegate(delegate);
		return store;
	}

	public static KeyValueStore get() {
		KeyValueStore singleton = Registry.checkSingleton(KeyValueStore.class);
		if (singleton == null) {
			singleton = new KeyValueStore();
			Registry.registerSingleton(KeyValueStore.class, singleton);
		}
		return singleton;
	}

	protected PersistenceObjectStore objectStore;

	protected KeyValueStore() {
		super();
	}

	public void get(String key, AsyncCallback<String> valueCallback) {
		this.objectStore.get(key, valueCallback);
	}

	public void getKeysPrefixedBy(String keyPrefix,
			AsyncCallback<List<String>> completedCallback) {
		this.objectStore.getKeysPrefixedBy(keyPrefix, completedCallback);
	}

	public PersistenceObjectStore getObjectStore() {
		return this.objectStore;
	}

	public void put(String key, String value,
			AsyncCallback<Integer> idCallback) {
		this.objectStore.put(key, value, idCallback);
	}

	public void registerDelegate(PersistenceObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	public void remove(String key, AsyncCallback<Integer> completedCallback) {
		this.objectStore.remove(key, completedCallback);
	}
}
