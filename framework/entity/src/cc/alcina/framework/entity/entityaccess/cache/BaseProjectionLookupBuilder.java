package cc.alcina.framework.entity.entityaccess.cache;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.MultikeyMapBase.DelegateMapCreator;
import cc.alcina.framework.common.client.util.SortedMultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class BaseProjectionLookupBuilder {
	boolean sorted = false;

	private int depth;

	public BaseProjectionLookupBuilder sorted() {
		sorted = true;
		return this;
	}

	public BaseProjectionLookupBuilder unsorted() {
		sorted = false;
		return this;
	}

	public BaseProjectionLookupBuilder depth(int depth) {
		this.depth = depth;
		return this;
	}

	public <T> MultikeyMap<T> createMultikeyMap() {
		MultikeyMap<T> map = null;
		BplDelegateMapCreator mapCreator = new BplDelegateMapCreator();
		if (sorted) {
			map = new SortedMultikeyMap<T>(depth, 0, mapCreator);
		} else {
			map = new UnsortedMultikeyMap<T>(depth, 0, mapCreator);
		}
		return map;
	}

	public class BplDelegateMapCreator extends DelegateMapCreator {
		@Override
		public Map createMap(int depth, int parentDepth) {
			return sorted ? new Object2ObjectAVLTreeMap()
					: new Object2ObjectLinkedOpenHashMap();
		}

		@Override
		public boolean isSorted(Map m) {
			return !(m instanceof Object2ObjectLinkedOpenHashMap) && super.isSorted(m);
		}
	}
}
