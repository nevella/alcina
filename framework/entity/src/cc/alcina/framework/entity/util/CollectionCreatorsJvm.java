package cc.alcina.framework.entity.util;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;
import cc.alcina.framework.common.client.util.CollectionCreators.HashMapCreator;
import cc.alcina.framework.common.client.util.CollectionCreators.HashSetCreator;
import cc.alcina.framework.common.client.util.CollectionCreators.LinkedMapCreator;
import cc.alcina.framework.common.client.util.CollectionCreators.UnsortedMapCreator;
import cc.alcina.framework.common.client.util.CollectionCreators.WeakMapCreator;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class CollectionCreatorsJvm {
	@Reflected
	@Registration.Singleton(
		value = ConcurrentMapCreator.class,
		priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class ConcurrentMapCreatorJvm extends ConcurrentMapCreator {
		@Override
		public <K, V> Map<K, V> create() {
			return new ConcurrentHashMap<>();
		}
	}

	public static class DelegateMapCreatorConcurrentNoNulls
			implements DelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new ConcurrentHashMap<>();
		}
	}

	@Reflected
	@Registration.Singleton(
		value = CollectionCreators.HashMapCreator.class,
		priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class HashMapCreatorJvm extends HashMapCreator {
		@Override
		public <K, V> Map<K, V> copy(Map<K, V> toClone) {
			if (toClone instanceof Object2ObjectLinkedOpenHashMap) {
				return ((Object2ObjectLinkedOpenHashMap) toClone).clone();
			} else {
				return new Object2ObjectLinkedOpenHashMap<>(toClone);
			}
		}

		@Override
		public <K, V> Map<K, V> create() {
			return new Object2ObjectLinkedOpenHashMap<>();
		}

		@Override
		public <K, V> Map<K, V> create(int initialSize) {
			return new Object2ObjectLinkedOpenHashMap<>(
					Math.max(10, initialSize), Hash.FAST_LOAD_FACTOR);
		}
	}

	@Reflected
	@Registration.Singleton(
		value = CollectionCreators.HashSetCreator.class,
		priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class HashSetCreatorJvm extends HashSetCreator {
		@Override
		public <T> Set<T> create() {
			return new ObjectLinkedOpenHashSet<>();
		}
	}

	@Reflected
	@Registration.Singleton(
		value = CollectionCreators.HashMapCreator.class,
		priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class LinkedHashMapCreatorJvm extends LinkedMapCreator {
		@Override
		public <K, V> Map<K, V> create() {
			return new Object2ObjectLinkedOpenHashMap<>();
		}
	}

	@Registration.Singleton(
		value = UnsortedMapCreator.class,
		priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class UnsortedMapCreatorJvm extends UnsortedMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new Object2ObjectLinkedOpenHashMap<>();
		}
	}

	@Reflected
	@Registration.Singleton(
		value = CollectionCreators.WeakMapCreator.class,
		priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class WeakMapCreatorJvm extends WeakMapCreator {
		@Override
		public <K, V> Map<K, V> create() {
			return new WeakHashMap<>();
		}
	}
}
