package cc.alcina.framework.servlet.sync;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class KeyedObject<T> implements Serializable {
	public KeyedObject() {
	}

	public KeyedObject(T object, StringKeyProvider keyProvider) {
		this.object = object;
		this.keyProvider = keyProvider;
	}

	private StringKeyProvider<T> keyProvider;

	private T object;

	public StringKeyProvider<T> getKeyProvider() {
		return this.keyProvider;
	}

	public void setKeyProvider(StringKeyProvider<T> keyProvider) {
		this.keyProvider = keyProvider;
	}

	public T getObject() {
		return this.object;
	}

	public void setObject(T object) {
		this.object = object;
	}

	public String getKey() {
		return keyProvider.firstKey(object);
	}

	public Class<? extends Object> getType() {
		return object.getClass();
	}

	@Override
	public String toString() {
		return object.toString();
	}
}
