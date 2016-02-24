package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import cc.alcina.framework.common.client.util.SortedMultiset;

public class ConcurrentSortedMultiset<K, V extends Set>
		extends SortedMultiset<K, V> {
	@Override
	protected Set createSet() {
		return new ConcurrentSkipListSet();
	}

	@Override
	protected void createTopMap() {
		map = new ConcurrentHashMap<>();
	}
}
