package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.Reflections;

import com.totsp.gwittir.client.beans.Converter;

public class CachingMap<I, O> {
	private final Converter<I, O> converter;

	private Map<I, O> map;

	public CachingMap(Converter<I, O> converter) {
		this(converter, new LinkedHashMap<I, O>());
	}

	public CachingMap(final Class valueClass) {
		this(new Converter<I, O>() {
			@Override
			public O convert(I original) {
				return (O) Reflections.classLookup().newInstance(valueClass);
			}
		}, new LinkedHashMap<I, O>());
	}

	public CachingMap(Converter<I, O> converter, Map<I, O> map) {
		this.converter = converter;
		this.map = map;
	}

	public O get(I key) {
		if (!map.containsKey(key)) {
			map.put(key, converter.convert(key));
		}
		return map.get(key);
	}

	public Map<I, O> getMap() {
		return this.map;
	}
}
