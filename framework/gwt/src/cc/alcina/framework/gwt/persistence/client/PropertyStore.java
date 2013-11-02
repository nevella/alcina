package cc.alcina.framework.gwt.persistence.client;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class PropertyStore {
	public static PropertyStore createNonStandardPropertyStore(
			PersistenceObjectStore delegate) {
		PropertyStore store = new PropertyStore();
		store.registerDelegate(delegate);
		return store;
	}

	public static PropertyStore get() {
		PropertyStore singleton = Registry.checkSingleton(PropertyStore.class);
		if (singleton == null) {
			singleton = new PropertyStore();
			Registry.registerSingleton(PropertyStore.class, singleton);
		}
		return singleton;
	}

	protected PersistenceObjectStore objectStore;

	protected PropertyStore() {
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

	public void put(String key, String value, AsyncCallback<Integer> idCallback) {
		this.objectStore.put(key, value, idCallback);
	}

	public void registerDelegate(PersistenceObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	public void remove(String key, AsyncCallback<Integer> completedCallback) {
		this.objectStore.remove(key, completedCallback);
	}
}
