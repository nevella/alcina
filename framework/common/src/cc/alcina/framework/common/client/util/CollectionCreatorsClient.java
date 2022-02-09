package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.google.gwt.core.client.GWT;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueSet;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.Registration;

// TODO - use fastidlookup, some sort of decorator for the sets
public class CollectionCreatorsClient {

    static boolean useJsMaps() {
        // GWT.isClient();
        return GWT.isScript();
    }

    @RegistryLocation(registryPoint = CollectionCreators.MultisetCreator.class, implementationType = ImplementationType.SINGLETON)
    @ClientInstantiable
    @Registration.Singleton(CollectionCreators.MultisetCreator.class)
    public static class CacheMultisetCreatorClient<K, V> implements CollectionCreators.MultisetCreator<K, V> {

        @Override
        public Multiset<K, Set<V>> create(Class<K> keyClass, Class<V> valueClass) {
            return useJsMaps() ? new SortedMultisetClient<>(keyClass, valueClass) : new SortedMultiset<>();
        }
    }

    public static class SortedMultisetClient<K, V> extends SortedMultiset<K, Set<V>> {

        public SortedMultisetClient(Class<K> keyClass, Class<V> valueClass) {
            map = useJsMaps() && keyClass != null ? JsUniqueMap.create() : new LinkedHashMap<>();
        }

        @Override
        protected Set createSet() {
            // FIXME - 2022 - that ain't sorted...
            return useJsMaps() ? new JsUniqueSet(Long.class) : new TreeSet<Long>();
        }

        @Override
        protected void createTopMap() {
        }
    }

    @RegistryLocation(registryPoint = CollectionCreators.TypedMapCreator.class, implementationType = ImplementationType.SINGLETON)
    @ClientInstantiable
    @Registration.Singleton(CollectionCreators.TypedMapCreator.class)
    public static class TypedMapCreatorCreatorClient<K, V> implements CollectionCreators.TypedMapCreator<K, V> {

        @Override
        public Map<K, V> create(Class<K> keyClass, Class<V> valueClass) {
            return useJsMaps() && keyClass != null ? JsUniqueMap.create() : new LinkedHashMap<>();
        }
    }
}
