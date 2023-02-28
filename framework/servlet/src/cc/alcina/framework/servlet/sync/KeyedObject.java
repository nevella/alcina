package cc.alcina.framework.servlet.sync;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.sync.StringKeyProvider;
import cc.alcina.framework.common.client.util.Ax;

@Bean
/*
 * Properties are not reflection-serializable (since currently too generic) -
 * revisit if needed
 */
public class KeyedObject<T> implements Serializable {
	private StringKeyProvider<T> keyProvider;

	private T object;

	public KeyedObject() {
	}

	public KeyedObject(T object, StringKeyProvider keyProvider) {
		this.object = object;
		this.keyProvider = keyProvider;
	}

	@JsonIgnore
	@AlcinaTransient
	public String getKey() {
		return keyProvider.firstKey(object);
	}

	@AlcinaTransient
	public StringKeyProvider<T> getKeyProvider() {
		return this.keyProvider;
	}

	@AlcinaTransient
	public T getObject() {
		return this.object;
	}

	public Class<? extends Object> getType() {
		return object.getClass();
	}

	public T resolveObject(Class<T> clazz, DetachedEntityCache resolver) {
		if (object instanceof String) {
			EntityLocator locator = EntityLocator.parseShort(clazz,
					(String) object);
			object = (T) resolver.get(locator);
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
