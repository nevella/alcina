package cc.alcina.framework.entity.util;

import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.ThrowingFunction;

public class CachingConcurrentMap<I, O> extends CachingMap<I, O> {
	public CachingConcurrentMap() {
	}

	public CachingConcurrentMap(ThrowingFunction<I, O> converter, int size) {
		super(converter, new ConcurrentHashMap<I, O>(size));
	}

	public static class CachingConcurrentLcMap
			extends CachingConcurrentMap<String, String> {
		public CachingConcurrentLcMap() {
			super(s -> s == null ? null : s.toLowerCase(), 999);
		}
	}
}
