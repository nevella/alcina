package cc.alcina.framework.servlet.sync;

import java.io.Serializable;

import cc.alcina.framework.common.client.domain.PrivateObjectCache;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.sync.StringKeyProvider;
import cc.alcina.framework.common.client.util.Ax;

public class KeyedObject<T> implements Serializable {
	private StringKeyProvider<T> keyProvider;

	private T object;

	public KeyedObject() {
	}

	public KeyedObject(T object, StringKeyProvider keyProvider) {
		this.object = object;
		this.keyProvider = keyProvider;
	}

	public String getKey() {
		return keyProvider.firstKey(object);
	}

	public StringKeyProvider<T> getKeyProvider() {
		return this.keyProvider;
	}

	public T getObject() {
		return this.object;
	}

	public Class<? extends Object> getType() {
		return object.getClass();
	}

	public T resolveObject(Class<T> clazz, PrivateObjectCache resolver) {
		if (object instanceof String) {
			HiliLocator locator = HiliLocator.parseShort(clazz,
					(String) object);
			object = resolver.get(locator);
			if (object == null) {
				throw Ax.runtimeException("Cannot resolve: %s", locator);
			}
		}
		return object;
	}

	public void setKeyProvider(StringKeyProvider<T> keyProvider) {
		this.keyProvider = keyProvider;
	}

	public void setObject(T object) {
		this.object = object;
	}

	@Override
	public String toString() {
		return object == null ? "(null object)" : object.toString();
	}
}
