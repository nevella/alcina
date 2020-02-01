package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Comparator;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.DelegateMapCreator;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.TransactionalMap;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.TransactionalTreeMap;

public class BaseProjectionSupport {
	public static class BplDelegateMapCreatorFastUnsorted
			extends DelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new TransactionalMap(Object.class, Object.class);
		}
	}

	@RegistryLocation(registryPoint = BaseProjectionLookupBuilder.BplDelegateMapCreator.class)
	public static class BplDelegateMapCreatorFastUtil
			extends BaseProjectionLookupBuilder.BplDelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			if (getBuilder().getCreators() != null) {
				Map map = (Map) getBuilder().getCreators()[depthFromRoot].get();
				Preconditions.checkState(map instanceof TransactionalMap);
				return map;
			}
			if (getBuilder().isNavigable()) {
				return new TransactionalTreeMap(Object.class, Object.class,
						Comparator.naturalOrder());
			} else {
				if (getBuilder().isSorted()) {
					return new TransactionalTreeMap(Object.class, Object.class,
							Comparator.naturalOrder());
				} else {
					if (depthFromRoot > 0 && depth == 1) {
						return new TransactionalMap(Object.class, Object.class);
					} else {
						return new TransactionalMap(Object.class, Object.class);
					}
				}
			}
		}
	}

	public static class Int2IntOpenHashMapCreator
			implements BaseProjectionLookupBuilder.MapCreator {
		@Override
		public Map get() {
			throw new UnsupportedOperationException();
			// return new Int2IntOpenHashMap();
		}
	}

	public static class Int2ObjectOpenHashMapCreator
			implements BaseProjectionLookupBuilder.MapCreator {
		@Override
		public Map get() {
			throw new UnsupportedOperationException();
			// return new Int2ObjectOpenHashMap();
		}
	}
}
