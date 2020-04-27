package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.Comparator;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder;
import cc.alcina.framework.common.client.domain.ReverseDateProjection.TreeMapRevCreator;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;
import cc.alcina.framework.common.client.util.NullFriendlyComparatorWrapper;

public class BaseProjectionSupportMvcc {
	public static class BplDelegateMapCreatorFastUnsorted
			implements DelegateMapCreator {
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
						new NullFriendlyComparatorWrapper(
								Comparator.reverseOrder()));
			} else {
				if (getBuilder().isSorted()) {
					return new TransactionalTreeMap(Object.class, Object.class,
							new NullFriendlyComparatorWrapper(
									Comparator.reverseOrder()));
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
			implements CollectionCreators.MapCreator {
		@Override
		public Map get() {
			throw new UnsupportedOperationException();
			// return new Int2IntOpenHashMap();
		}
	}

	public static class Int2ObjectOpenHashMapCreator
			implements CollectionCreators.MapCreator {
		@Override
		public Map get() {
			throw new UnsupportedOperationException();
			// return new Int2ObjectOpenHashMap();
		}
	}

	@RegistryLocation(registryPoint = TreeMapRevCreator.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class TreeMapRevCreatorImpl extends TreeMapRevCreator {
		@Override
		public Map get() {
			return new TransactionalTreeMap(types.get(0), types.get(1),
					new NullFriendlyComparatorWrapper(
							Comparator.reverseOrder()));
		}
	}
}
