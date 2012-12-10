package cc.alcina.framework.gwt.persistence.client;

public class PropertyStore {
	private PropertyStore() {
		super();
	}

	private static PropertyStore theInstance;
	private ObjectStore objectStore;

	public static PropertyStore get() {
		if (theInstance == null) {
			theInstance = new PropertyStore();
		}
		return theInstance;
	}
	public void registerDelegate(ObjectStore objectStore){
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
}
