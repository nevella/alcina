package cc.alcina.framework.gwt.client.data.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.DomainLookup;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreIdMapCreator;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreLongSetCreator;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreMultisetCreator;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStorePrivateObjectCacheCreator;
import cc.alcina.framework.common.client.domain.PrivateObjectCache;
import cc.alcina.framework.common.client.domain.PrivateObjectCache.PrivateObjectCacheSingleClass;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueSet;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.SortedMultiset;

//TODO - use fastidlookup, some sort of decorator for the sets
public class DataCollectionSuppliers {
	static boolean useJsMaps() {
		return GWT.isScript();// GWT.isClient();
	}

	@RegistryLocation(registryPoint = DomainStoreIdMapCreator.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class CacheIdMapCreatorClient
			implements DomainStoreIdMapCreator {
		@Override
		public Map<Long, HasIdAndLocalId> get() {
			return useJsMaps() ? JsUniqueMap.create(Long.class, false)
					: new LinkedHashMap<Long, HasIdAndLocalId>();
		}
	}

	@RegistryLocation(registryPoint = DomainStoreLongSetCreator.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class CacheLongSetCreatorClient
			implements DomainStoreLongSetCreator {
		@Override
		public Set<Long> get() {
			return useJsMaps() ? new JsUniqueSet(Long.class)
					: new TreeSet<Long>();
		}
	}

	@RegistryLocation(registryPoint = DomainStoreMultisetCreator.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class CacheMultisetCreatorClient<T>
			implements DomainStoreMultisetCreator<T> {
		@Override
		public SortedMultiset<T, Set<Long>> get(DomainLookup cacheLookup,
				boolean concurrent) {
			return useJsMaps() ? new SortedMultisetClient<>(cacheLookup)
					: new SortedMultiset<>();
		}
	}

	@RegistryLocation(registryPoint = DomainStorePrivateObjectCacheCreator.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class CachePrivateObjectCacheCreatorClient
			implements DomainStorePrivateObjectCacheCreator {
		@Override
		public PrivateObjectCache get() {
			return new PrivateObjectCacheSingleClass();
		}
	}

	public static class SortedMultisetClient<K, V extends Set>
			extends SortedMultiset<K, V> {
		public SortedMultisetClient(DomainLookup cacheLookup) {
			Class templateClass = cacheLookup.getListenedClass();
			Object instance = Reflections.classLookup()
					.newInstance(templateClass);
			Class indexClass = cacheLookup.getPropertyPathAccesor()
					.getChainedPropertyType(instance);
			map = useJsMaps() && indexClass != null
					? JsUniqueMap.create(indexClass, false)
					: new LinkedHashMap<>();
		}

		@Override
		protected Set createSet() {
			return useJsMaps() ? new JsUniqueSet(Long.class)
					: new TreeSet<Long>();
		}

		@Override
		protected void createTopMap() {
		}
	}
}
