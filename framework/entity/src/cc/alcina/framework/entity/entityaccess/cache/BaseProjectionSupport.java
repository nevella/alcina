package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Map;
import java.util.TreeMap;

import cc.alcina.framework.common.client.cache.BaseProjectionLookupBuilder;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.MultikeyMapBase.DelegateMapCreator;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class BaseProjectionSupport {
	public static class Object2ObjectOpenHashMapCreator
			implements BaseProjectionLookupBuilder.MapCreator {
		@Override
		public Map get() {
			return new Object2ObjectOpenHashMap();
		}
	}

	public static class Int2IntOpenHashMapCreator
			implements BaseProjectionLookupBuilder.MapCreator {
		@Override
		public Map get() {
			return new Int2IntOpenHashMap();
		}
	}

	public static class Int2ObjectOpenHashMapCreator
			implements BaseProjectionLookupBuilder.MapCreator {
		@Override
		public Map get() {
			return new Int2ObjectOpenHashMap();
		}
	}

	public static class BplDelegateMapCreatorFastUnsorted
			extends DelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot) {
			return new Object2ObjectLinkedOpenHashMap();
		}
	}

	@RegistryLocation(registryPoint = BaseProjectionLookupBuilder.BplDelegateMapCreator.class)
	public static class BplDelegateMapCreatorFastUtil
			extends BaseProjectionLookupBuilder.BplDelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot) {
			if (getBuilder().getCreators() != null) {
				return getBuilder().getCreators()[depthFromRoot].get();
			}
			if (getBuilder().isNavigable()) {
				return new TreeMap();
			} else {
				return getBuilder().isSorted() ? new Object2ObjectAVLTreeMap()
						: new Object2ObjectLinkedOpenHashMap();
			}
		}

		@Override
		public boolean isSorted(Map m) {
			return !(m instanceof Object2ObjectLinkedOpenHashMap)
					&& super.isSorted(m);
		}
	}
}
