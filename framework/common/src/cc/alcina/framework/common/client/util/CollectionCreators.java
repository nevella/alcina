package cc.alcina.framework.common.client.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.trie.KeyAnalyzer;
import cc.alcina.framework.common.client.util.trie.MultiTrie;

public class CollectionCreators {
	public static class Bootstrap {
		private static ConcurrentMapCreator concurrentClassMapCreator = new ConcurrentMapCreator();

		public static void setConcurrentClassMapCreator(
				ConcurrentMapCreator concurrentClassMapCreator) {
			CollectionCreators.Bootstrap.concurrentClassMapCreator = concurrentClassMapCreator;
		}

		private static ConcurrentMapCreator concurrentStringMapCreator = new ConcurrentMapCreator();

		public static void setConcurrentStringMapCreator(
				ConcurrentMapCreator concurrentStringMapCreator) {
			CollectionCreators.Bootstrap.concurrentStringMapCreator = concurrentStringMapCreator;
		}

		public static <T> Map<Class, T> createConcurrentClassMap() {
			return concurrentClassMapCreator.create();
		}

		public static <T> Map<String, T> createConcurrentStringMap() {
			return concurrentStringMapCreator.create();
		}
	}

	@RegistryLocation(registryPoint = ConcurrentMapCreator.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
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

	@RegistryLocation(registryPoint = CollectionCreators.HashMapCreator.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class HashMapCreator {
		public <K, V> Map<K, V> copy(Map<K, V> from) {
			return new HashMap<>(from);
		}

		public <K, V> Map<K, V> create() {
			return new HashMap<>();
		}
	}

	public interface MapCreator<K, V> extends Supplier<Map<K, V>> {
	}

	public static interface MultisetCreator<K, V> {
		public Multiset<K, Set<V>> create(Class<K> keyClass,
				Class<V> valueClass);
	}

	@RegistryLocation(registryPoint = MultiTrieCreator.class, implementationType = ImplementationType.INSTANCE)
	public static class MultiTrieCreator {
		public MultiTrieCreator() {
		}

		public <K, E extends Entity> MultiTrie<K, Set<E>> create(
				KeyAnalyzer<? super K> keyAnalyzer, Class<E> entityClass) {
			return new MultiTrie<K, Set<E>>(keyAnalyzer);
		}
	}

	@RegistryLocation(registryPoint = TreeMapCreator.class, implementationType = ImplementationType.INSTANCE)
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

	@RegistryLocation(registryPoint = TreeMapRevCreator.class, implementationType = ImplementationType.INSTANCE)
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

	@RegistryLocation(registryPoint = UnsortedMapCreator.class, implementationType = ImplementationType.SINGLETON)
	public static class UnsortedMapCreator implements DelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new LinkedHashMap<>();
		}
	}
}
