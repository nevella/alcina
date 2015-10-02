package cc.alcina.framework.entity.entityaccess.cache;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.MultikeyMapBase.DelegateMapCreator;
import cc.alcina.framework.common.client.util.SortedMultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class BaseProjectionLookupBuilder {
	boolean sorted = false;

	private int depth;

	private MapCreator[] creators;

	private boolean navigable;

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

	public BaseProjectionLookupBuilder mapCreators(MapCreator... creators) {
		if (creators.length != depth) {
			throw new RuntimeException(
					"Mismatched creator array length and depth");
		}
		this.creators = creators;
		return this;
	}

	public interface MapCreator extends Supplier<Map> {
	}

	public static class Object2ObjectOpenHashMapCreator implements MapCreator {
		@Override
		public Map get() {
			return new Object2ObjectOpenHashMap();
		}
	}

	public static class Int2IntOpenHashMapCreator implements MapCreator {
		@Override
		public Map get() {
			return new Int2IntOpenHashMap();
		}
	}

	public static class Int2ObjectOpenHashMapCreator implements MapCreator {
		@Override
		public Map get() {
			return new Int2ObjectOpenHashMap();
		}
	}

	public class BplDelegateMapCreator extends DelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot) {
			if (creators != null) {
				return creators[depthFromRoot].get();
			}
			if (navigable) {
				return new TreeMap();
			} else {
				return sorted ? new Object2ObjectAVLTreeMap()
						: new Object2ObjectLinkedOpenHashMap();
			}
		}

		@Override
		public boolean isSorted(Map m) {
			return !(m instanceof Object2ObjectLinkedOpenHashMap)
					&& super.isSorted(m);
		}
	}

	public BaseProjectionLookupBuilder navigable() {
		navigable = true;
		sorted = true;
		return this;
	}
}
