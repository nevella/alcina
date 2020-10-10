package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder.BplDelegateMapCreator;
import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;
import cc.alcina.framework.common.client.util.CollectionCreators.MapCreator;
import cc.alcina.framework.common.client.util.NullFriendlyComparatorWrapper;
import cc.alcina.framework.common.client.util.trie.KeyAnalyzer;
import cc.alcina.framework.common.client.util.trie.MultiTrie;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class BaseProjectionSupportMvcc {
	public static class BplDelegateMapCreatorNonTransactional
			implements DelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new Object2ObjectLinkedOpenHashMap();
		}
	}

	@RegistryLocation(registryPoint = BplDelegateMapCreator.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class BplDelegateMapCreatorTransactional
			extends BplDelegateMapCreator {
		private boolean nonTransactionalDomain;

		public BplDelegateMapCreatorTransactional() {
			this.nonTransactionalDomain = IDomainStore
					.isNonTransactionalDomain();
		}

		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			if (getBuilder().getCreators() != null
					&& getBuilder().getCreators().length > depthFromRoot) {
				Map map = (Map) getBuilder().getCreators()[depthFromRoot].get();
				Preconditions.checkState((nonTransactionalDomain)
						^ (map instanceof TransactionalMap));
				return map;
			} else {
				if (nonTransactionalDomain) {
					return new Object2ObjectLinkedOpenHashMap<>();
				} else {
					return new TransactionalMap(Object.class, Object.class);
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

	@RegistryLocation(registryPoint = CollectionCreators.MultiTrieCreator.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class MultiTrieCreatorImpl
			extends CollectionCreators.MultiTrieCreator {
		@Override
		public <K, E extends Entity> MultiTrie<K, Set<E>> create(
				KeyAnalyzer<? super K> keyAnalyzer, Class<E> entityClass) {
			return new TransactionalMultiTrie<>(keyAnalyzer, entityClass);
		}
	}

	public static class Object2ObjectHashMapCreator
			implements CollectionCreators.MapCreator {
		@Override
		public Map get() {
			return new Object2ObjectLinkedOpenHashMap();
		}
	}

	public static class TransactionalObject2ObjectMapCreator
			implements MapCreator {
		@Override
		public Object get() {
			return new TransactionalMap(Object.class, Object.class);
		}
	}

	@RegistryLocation(registryPoint = CollectionCreators.TreeMapRevCreator.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class TreeMapRevCreatorImpl
			extends CollectionCreators.TreeMapRevCreator {
		@Override
		public Map get() {
			return new TransactionalTreeMap(types.get(0), types.get(1),
					new NullFriendlyComparatorWrapper(
							Comparator.reverseOrder()));
		}
	}
}
