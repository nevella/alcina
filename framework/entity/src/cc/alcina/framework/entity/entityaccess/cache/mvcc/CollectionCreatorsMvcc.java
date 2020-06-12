package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

public class CollectionCreatorsMvcc {
	@RegistryLocation(registryPoint = CollectionCreators.MultisetCreator.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class DomainStoreMultisetCreator<K, V>
			implements CollectionCreators.MultisetCreator<K, V> {
		private boolean nonTransactionalDomain;

		public DomainStoreMultisetCreator() {
			this.nonTransactionalDomain = DomainStore
					.isNonTransactionalDomain();
		}

		@Override
		public Multiset<K, Set<V>> create(Class<K> keyClass,
				Class<V> valueClass) {
			Preconditions.checkNotNull(keyClass);
			Preconditions.checkNotNull(valueClass);
			if (nonTransactionalDomain) {
				return new Multiset<>();
			} else {
				return new TransactionalMultiset<>(keyClass, valueClass);
			}
		}

		private final static class TransactionalMultiset<K, V>
				extends Multiset<K, Set<V>> {
			@SuppressWarnings("unused")
			private Class<K> keyClass;

			private Class<V> valueClass;

			public TransactionalMultiset(Class<K> keyClass,
					Class<V> valueClass) {
				this.keyClass = keyClass;
				this.valueClass = valueClass;
				map = new TransactionalMap(keyClass, Set.class);
			}

			@Override
			protected Set<V> createSet() {
				Class<? extends Entity> entityClass = (Class<? extends Entity>) valueClass;
				return new TransactionalSet(entityClass);
			}

			@Override
			protected void createTopMap() {
				// do it in *our* init
			}
		}
	}

	@RegistryLocation(registryPoint = CollectionCreators.TypedMapCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	@ClientInstantiable
	public static class TypedMapCreatorCreatorMvcc<K, V>
			implements CollectionCreators.TypedMapCreator<K, V> {
		@Override
		public Map<K, V> create(Class<K> keyClass, Class<V> valueClass) {
			return new TransactionalMap(keyClass, valueClass);
		}
	}
}
