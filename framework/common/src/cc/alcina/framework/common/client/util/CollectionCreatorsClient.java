package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueSet;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

public class CollectionCreatorsClient {
	static boolean useJsMaps() {
		return GWT.isScript();
	}

	@Reflected
	@Registration.Singleton(CollectionCreators.MultisetCreator.class)
	public static class CacheMultisetCreatorClient<K, V>
			implements CollectionCreators.MultisetCreator<K, V> {
		@Override
		public Multiset<K, Set<V>> create(Class<K> keyClass,
				Class<V> valueClass) {
			return useJsMaps() ? new MultisetClient<>(keyClass, valueClass)
					: new SortedMultiset<>();
		}
	}

	public static class MultisetClient<K, V> extends Multiset<K, Set<V>> {
		public MultisetClient(Class<K> keyClass, Class<V> valueClass) {
			map = useJsMaps() && keyClass != null ? JsUniqueMap.create()
					: new LinkedHashMap<>();
		}

		@Override
		protected Set createSet() {
			return useJsMaps() ? new JsUniqueSet(Long.class)
					: new LinkedHashSet<Long>();
		}

		@Override
		protected void createTopMap() {
		}
	}

	public static interface TransactionalSetCreator<E extends Entity> {
		public Set<E> create(Class<E> valueClass);
	}

	@Reflected
	public static class TransactionalSetCreatorClient<E extends Entity>
			implements CollectionCreators.TransactionalSetCreator<E> {
		@Override
		public Set<E> create(Class<E> valueClass) {
			return new LinkedHashSet<>();
		}
	}

	@Reflected
	@Registration.Singleton(CollectionCreators.TypedMapCreator.class)
	public static class TypedMapCreatorCreatorClient<K, V>
			implements CollectionCreators.TypedMapCreator<K, V> {
		@Override
		public Map<K, V> create(Class<K> keyClass, Class<V> valueClass) {
			return useJsMaps() && keyClass != null ? JsUniqueMap.create()
					: new LinkedHashMap<>();
		}
	}
}
