package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet.DegenerateCreator;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.Multiset;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class CollectionCreatorsMvcc {
	public static class DegenerateCreatorMvcc extends DegenerateCreator {
		@Override
		public Set copy(Set degenerate) {
			return ((ObjectOpenHashSet) degenerate).clone();
		}

		@Override
		public Set create() {
			return new ObjectOpenHashSet<>(60);
		}
	}

	@RegistryLocation(registryPoint = CollectionCreators.MultisetCreator.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	@Registration(value = CollectionCreators.MultisetCreator.class, priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class DomainStoreMultisetCreator<K, V>
			implements CollectionCreators.MultisetCreator<K, V> {
		private boolean nonTransactionalDomain;

		public DomainStoreMultisetCreator() {
			this.nonTransactionalDomain = IDomainStore
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
	}

	@RegistryLocation(registryPoint = CollectionCreators.TypedMapCreator.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	@ClientInstantiable
	@Registration.Singleton(value = CollectionCreators.TypedMapCreator.class, priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class TypedMapCreatorCreatorMvcc<K, V>
			implements CollectionCreators.TypedMapCreator<K, V> {
		@Override
		public Map<K, V> create(Class<K> keyClass, Class<V> valueClass) {
			return new TransactionalMap(keyClass, valueClass);
		}
	}
}
