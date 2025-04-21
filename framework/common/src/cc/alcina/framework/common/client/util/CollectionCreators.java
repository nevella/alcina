package cc.alcina.framework.common.client.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.trie.KeyAnalyzer;
import cc.alcina.framework.common.client.util.trie.MultiTrie;

/*
 * FIXME - dirndl 1x1g - change to Maps, merge with AlcinaCollections (similar
 * to Guava, really)
 *
 * These should use gwt (js) maps where appropriate
 */
public class CollectionCreators {
	public static class Bootstrap {
		private static ConcurrentMapCreator concurrentClassMapCreator = new ConcurrentMapCreator();

		private static HashMapCreator hashMapCreator = new HashMapCreator();

		private static LinkedMapCreator linkedMapCreator = new LinkedMapCreator();

		private static ConcurrentMapCreator concurrentStringMapCreator = new ConcurrentMapCreator();

		public static <T> Map<Class, T> createConcurrentClassMap() {
			return concurrentClassMapCreator.create();
		}

		public static <T> Map<String, T> createConcurrentStringMap() {
			return concurrentStringMapCreator.create();
		}

		public static HashMapCreator getHashMapCreator() {
			return hashMapCreator;
		}

		public static LinkedMapCreator getLinkedMapCreator() {
			return linkedMapCreator;
		}

		public static void setConcurrentClassMapCreator(
				ConcurrentMapCreator concurrentClassMapCreator) {
			CollectionCreators.Bootstrap.concurrentClassMapCreator = concurrentClassMapCreator;
		}

		public static void setConcurrentStringMapCreator(
				ConcurrentMapCreator concurrentStringMapCreator) {
			CollectionCreators.Bootstrap.concurrentStringMapCreator = concurrentStringMapCreator;
		}

		public static void setHashMapCreator(HashMapCreator hashMapCreator) {
			Bootstrap.hashMapCreator = hashMapCreator;
		}

		public static void
				setLinkedMapCreator(LinkedMapCreator linkedMapCreator) {
			Bootstrap.linkedMapCreator = linkedMapCreator;
		}
	}

	@Reflected
	@Registration.Singleton
	public static class ConcurrentMapCreator {
		public <K, V> Map<K, V> create() {
			return new LinkedHashMap<>();
		}
	}

	public interface DelegateMapCreator {
		Map createDelegateMap(int depthFromRoot, int depth);

		default boolean isSorted(Map m) {
			return m instanceof SortedMap;
		}
	}

	@Reflected
	@Registration.Singleton(CollectionCreators.HashMapCreator.class)
	public static class HashMapCreator {
		public <K, V> Map<K, V> copy(Map<K, V> from) {
			return new HashMap<>(from);
		}

		public <K, V> Map<K, V> create() {
			return new HashMap<>();
		}

		public <K, V> Map<K, V> create(int initialSize) {
			return new HashMap<>();
		}
	}

	@Reflected
	@Registration.Singleton(CollectionCreators.HashSetCreator.class)
	public static class HashSetCreator {
		public <T> Set<T> create() {
			return new HashSet<>();
		}
	}

	@Registration.Singleton
	public static class LinkedMapCreator {
		public static CollectionCreators.LinkedMapCreator get() {
			return Registry.impl(CollectionCreators.LinkedMapCreator.class);
		}

		public <K, V> Map<K, V> create() {
			return new LinkedHashMap<>();
		}
	}

	public interface MapCreator<K, V> extends Supplier<Map<K, V>> {
	}

	public static interface MultisetCreator<K, V> {
		public Multiset<K, Set<V>> create(Class<K> keyClass,
				Class<V> valueClass);
	}

	@Registration(MultiTrieCreator.class)
	public static class MultiTrieCreator {
		public MultiTrieCreator() {
		}

		public <K, E extends Entity> MultiTrie<K, Set<E>> create(
				KeyAnalyzer<? super K> keyAnalyzer, Class<E> entityClass) {
			return new MultiTrie<K, Set<E>>(keyAnalyzer);
		}
	}

	public static interface TransactionalSetCreator<E extends Entity> {
		public Set<E> create(Class<E> valueClass);
	}

	@Registration(TreeMapCreator.class)
	public static class TreeMapCreator implements MapCreator {
		public List<Class> types;

		public TreeMapCreator() {
		}

		@Override
		public Map get() {
			return new TreeMap();
		}

		public TreeMapCreator withTypes(List<Class> types) {
			this.types = types;
			return this;
		}
	}

	@Registration(TreeMapRevCreator.class)
	public static class TreeMapRevCreator implements MapCreator {
		public List<Class> types;

		public TreeMapRevCreator() {
		}

		@Override
		public Map get() {
			return new TreeMap(Comparator.reverseOrder());
		}

		public TreeMapRevCreator withTypes(List<Class> types) {
			this.types = types;
			return this;
		}
	}

	public static interface TypedMapCreator<K, V> {
		public Map<K, V> create(Class<K> keyClass, Class<V> valueClass);
	}

	@Registration.Singleton
	public static class UnsortedMapCreator implements DelegateMapCreator {
		public Map create() {
			return createDelegateMap(0, 0);
		}

		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new LinkedHashMap<>();
		}
	}

	@Reflected
	@Registration.Singleton(CollectionCreators.WeakMapCreator.class)
	/*
	 * FIXME - reflection - default (dev only, so not a big drama) impl is in
	 * fact non-weak
	 */
	public static class WeakMapCreator {
		public <K, V> Map<K, V> create() {
			return new LinkedHashMap<>();
		}
	}

	@Reflected
	@Registration.Singleton(CollectionCreators.CoarseIntHashMapCreator.class)
	/*
	 * FIXME - reflection - default (dev only, so not a big drama) impl is in
	 * fact non-weak
	 */
	public static class CoarseIntHashMapCreator {
		public <V> Map<Integer, V> create() {
			return GWT.isScript() ? JsUniqueMap.create()
					: new LinkedHashMap<>();
		}
	}
}
