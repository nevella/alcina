package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;

public class CachingMap<I, O> {
	private final ThrowingFunction<I, O> function;

	private Map<I, O> map;

	public CachingMap(ThrowingFunction<I, O> function) {
		this(function, new LinkedHashMap<I, O>());
	}

	public CachingMap(final Class valueClass) {
		this(new ThrowingFunction<I, O>() {
			@Override
			public O apply(I original) {
				return (O) Reflections.classLookup().newInstance(valueClass);
			}
		}, new LinkedHashMap<I, O>());
	}

	public CachingMap(ThrowingFunction<I, O> function, Map<I, O> map) {
		this.function = function;
		this.map = map;
	}

	public O get(I key) {
		if (!map.containsKey(key)) {
			try {
				map.put(key, function.apply(key));
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			
		}
		return map.get(key);
	}

	public void remove(I key) {
		map.remove(key);
	}

	public void put(I key, O value) {
		map.put(key, value);
	}

	public Map<I, O> getMap() {
		return this.map;
	}

	public void clear() {
		this.map.clear();
	}
}
