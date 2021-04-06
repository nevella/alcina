package cc.alcina.framework.entity.util;

import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.util.Multimap;

public class SynchronizedMultimap {
	public static <K, V extends List> Multimap<K, V> synchronizedMultimap() {
		return new Synchronized<>(new Multimap<>());
	}

	private static class Synchronized<K, V extends List>
			extends Multimap<K, V> {
		private Synchronized(Multimap<K, V> delegate) {
			map = Collections.synchronizedMap(delegate.provideMap());
		}
	}
}
