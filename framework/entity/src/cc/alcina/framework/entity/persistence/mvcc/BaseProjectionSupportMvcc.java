package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder.BplDelegateMapCreator;
import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;
import cc.alcina.framework.common.client.util.CollectionCreators.MapCreator;
import cc.alcina.framework.common.client.util.NullFriendlyComparatorWrapper;
import cc.alcina.framework.common.client.util.trie.KeyAnalyzer;
import cc.alcina.framework.common.client.util.trie.MultiTrie;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

/*
 * Because these collection creators subclass their parents, but should only be
 * used if in a transactional environment, their declarative registrations are
 * removed and added by Mvcc.init code during transactional environment setup
 */
public class BaseProjectionSupportMvcc {
	public static class BplDelegateMapCreatorNonTransactional
			implements DelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new Object2ObjectLinkedOpenHashMap();
		}
	}

	@Registration(
		value = BplDelegateMapCreator.class,
		priority = Registration.Priority.REMOVE)
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

	@Registration(
		value = CollectionCreators.MultiTrieCreator.class,
		priority = Registration.Priority.REMOVE)
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

	@Registration(
		value = CollectionCreators.TreeMapCreator.class,
		priority = Registration.Priority.REMOVE)
	public static class TreeMapCreatorImpl
			extends CollectionCreators.TreeMapCreator {
		private boolean pureTransactional;

		@Override
		public Map get() {
			return new TransactionalTreeMap(types.get(0), types.get(1),
					new NullFriendlyComparatorWrapper(
							Comparator.naturalOrder()))
									.withPureTransactional(pureTransactional);
		}

		public TreeMapCreatorImpl
				withPureTransactional(boolean pureTransactional) {
			this.pureTransactional = pureTransactional;
			return this;
		}
	}

	public static class TreeMapCreatorNonTransactional<K, V>
			implements CollectionCreators.MapCreator<K, V> {
		private Comparator<K> cmp;

		@Override
		public Map<K, V> get() {
			return cmp == null ? new Object2ObjectAVLTreeMap<>()
					: new Object2ObjectAVLTreeMap<>(cmp);
		}

		public TreeMapCreatorNonTransactional<K, V>
				withComparator(Comparator cmp) {
			this.cmp = cmp;
			return this;
		}
	}

	// force imperative registration
	@Registration(
		value = CollectionCreators.TreeMapRevCreator.class,
		priority = Registration.Priority.REMOVE)
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
