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
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.trie.KeyAnalyzer;
import cc.alcina.framework.common.client.util.trie.MultiTrie;

/*
 * FIXME - 2022 - change to Maps (similar to Guava, really)
 */
public class CollectionCreators {
	public static class Bootstrap {
		private static ConcurrentMapCreator concurrentClassMapCreator = new ConcurrentMapCreator();

		private static HashMapCreator hashMapCreator = new HashMapCreator();

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
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new LinkedHashMap<>();
		}
	}
}
