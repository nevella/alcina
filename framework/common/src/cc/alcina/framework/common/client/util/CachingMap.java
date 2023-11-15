package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.Reflections;


public class CachingMap<I, O> {
	private ThrowingFunction<I, O> function;

	private Map<I, O> map;

	// for serialization
	public CachingMap() {
	}

	public CachingMap(final Class valueClass) {
		this(new ThrowingFunction<I, O>() {
			@Override
			public O apply(I original) {
				return (O) Reflections.newInstance(valueClass);
			}
		}, new LinkedHashMap<I, O>());
	}

	public CachingMap(ThrowingFunction<I, O> function) {
		this(function, new LinkedHashMap<I, O>());
	}

	public CachingMap(ThrowingFunction<I, O> function, Map<I, O> map) {
		this.map = map;
		this.setFunction(function);
	}

	public void clear() {
		this.map.clear();
	}

	public O get(I key) {
		// optimise - mostly a key won't map to null;
		O o = map.get(key);
		if (o != null) {
			return o;
		}
		if (!map.containsKey(key)) {
			try {
				map.put(key, getFunction().apply(key));
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return map.get(key);
	}

	public ThrowingFunction<I, O> getFunction() {
		return function;
	}

	public Map<I, O> getMap() {
		return this.map;
	}

	public void merge(CachingMap<I, O> otherCachingMap,
			BiConsumer<O, O> merger) {
		otherCachingMap.getMap().entrySet().forEach(e -> {
			O to = get(e.getKey());
			// it's a method on the "to" object, so that shd be first argument
			merger.accept(to, e.getValue());
		});
	}

	public void put(I key, O value) {
		map.put(key, value);
	}

	public void remove(I key) {
		map.remove(key);
	}

	public void setFunction(ThrowingFunction<I, O> function) {
		this.function = function;
		if (this.map == null) {
			this.map = new LinkedHashMap<>();
		}
	}

	public int size() {
		return map.size();
	}

	@Override
	public String toString() {
		return map.toString();
	}

	public Collection<O> values() {
		return map.values();
	}

	public static class CachingLcMap extends CachingMap<String, String> {
		public CachingLcMap() {
			super(s -> s == null ? null : s.toLowerCase());
		}
	}
}
