package cc.alcina.framework.common.client.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.totsp.gwittir.client.beans.Converter;

public class CachingMap<I, O> {
	private final Converter<I, O> converter;

	private Map<I, O> map;

	public CachingMap(Converter<I, O> converter) {
		this(converter, new LinkedHashMap<I, O>());
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
}
