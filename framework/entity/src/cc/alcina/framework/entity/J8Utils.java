package cc.alcina.framework.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class J8Utils {
	public static <T> Comparator<T>
			comparingCaseInsensitive(Function<? super T, String> keyExtractor) {
		Objects.requireNonNull(keyExtractor);
		return (Comparator<T> & Serializable) (c1,
				c2) -> String.CASE_INSENSITIVE_ORDER.compare(
						keyExtractor.apply(c1), keyExtractor.apply(c2));
	}

	public static <T> Collector<Collection<T>, ?, List<T>> toItemList() {
		return new ToItemListCollector();
	}

	public static <T> Collector<Collection<T>, ?, Stream<T>> toItemStream() {
		return new ToItemStreamCollector();
	}

	public static <T, K> Collector<T, ?, Map<K, T>>
			toKeyMap(Function<? super T, ? extends K> keyMapper) {
		return new ToMapCollector(keyMapper, LinkedHashMap::new);
	}

	public static <T, K, U> Collector<T, ?, Multimap<K, List<U>>>
			toKeyMultimap(Function<? super T, ? extends K> keyMapper) {
		return new ToMultimapCollector(keyMapper, t -> t, Multimap::new);
	}

	public static <T> Collector<T, ?, Set<T>> toLinkedHashSet() {
		return new ToLinkedHashSetCollector<>();
	}

	public static <T, K, U> Collector<T, ?, Multimap<K, List<U>>> toMultimap(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return new ToMultimapCollector(keyMapper, valueMapper, Multimap::new);
	}

	public static <T, V> Collector<T, ?, UnsortedMultikeyMap<V>>
			toUnsortedMultikeyMapCollector(Function<? super T, Object[]> mapper,
					int depth) {
		return new ToUnsortedMultikeyMapCollector(mapper, depth);
	}

	private static class ToItemListCollector<T>
			implements Collector<Collection<T>, List<T>, List<T>> {
		public ToItemListCollector() {
		}

		@Override
		public BiConsumer<List<T>, Collection<T>> accumulator() {
			return (list, coll) -> list.addAll(coll);
		}

		@Override
		public Set<java.util.stream.Collector.Characteristics>
				characteristics() {
			return EnumSet.of(Characteristics.IDENTITY_FINISH);
		}

		@Override
		public BinaryOperator<List<T>> combiner() {
			return (left, right) -> {
				left.addAll(right);
				return left;
			};
		}

		@Override
		public Function<List<T>, List<T>> finisher() {
			return null;
		}

		@Override
		public Supplier<List<T>> supplier() {
			return ArrayList::new;
		}
	}

	private static class ToItemStreamCollector<T>
			implements Collector<Collection<T>, List<T>, Stream<T>> {
		public ToItemStreamCollector() {
		}

		@Override
		public BiConsumer<List<T>, Collection<T>> accumulator() {
			return (list, coll) -> list.addAll(coll);
		}

		@Override
		public Set<java.util.stream.Collector.Characteristics>
				characteristics() {
			return EnumSet.noneOf(Characteristics.class);
		}

		@Override
		public BinaryOperator<List<T>> combiner() {
			return (left, right) -> {
				left.addAll(right);
				return left;
			};
		}

		@Override
		public Function<List<T>, Stream<T>> finisher() {
			return List::stream;
		}

		@Override
		public Supplier<List<T>> supplier() {
			return ArrayList::new;
		}
	}

	private static class ToLinkedHashSetCollector<T>
			implements java.util.stream.Collector<T, Set<T>, Set<T>> {
		public ToLinkedHashSetCollector() {
		}

		@Override
		public BiConsumer<Set<T>, T> accumulator() {
			return (set, t) -> set.add(t);
		}

		@Override
		public Set<java.util.stream.Collector.Characteristics>
				characteristics() {
			return EnumSet.of(Characteristics.IDENTITY_FINISH);
		}

		@Override
		public BinaryOperator<Set<T>> combiner() {
			return (left, right) -> {
				left.addAll(right);
				return left;
			};
		}

		@Override
		public Function<Set<T>, Set<T>> finisher() {
			return null;
		}

		@Override
		public Supplier<Set<T>> supplier() {
			return () -> new LinkedHashSet<>();
		}
	}

	private static class ToLiSetCollector<T>
			implements java.util.stream.Collector<T, Set<T>, Set<T>> {
		public ToLiSetCollector() {
		}

		@Override
		public BiConsumer<Set<T>, T> accumulator() {
			return (set, t) -> set.add(t);
		}

		@Override
		public Set<java.util.stream.Collector.Characteristics>
				characteristics() {
			return EnumSet.of(Characteristics.IDENTITY_FINISH);
		}

		@Override
		public BinaryOperator<Set<T>> combiner() {
			return (left, right) -> {
				left.addAll(right);
				return left;
			};
		}

		@Override
		public Function<Set<T>, Set<T>> finisher() {
			return null;
		}

		@Override
		public Supplier<Set<T>> supplier() {
			return () -> new LiSet();
		}
	}

	private static class ToMapCollector<T, K>
			implements java.util.stream.Collector<T, Map<K, T>, Map<K, T>> {
		private Function<? super T, ? extends K> keyMapper;

		private Supplier<Map<K, T>> supplier;

		public ToMapCollector(Function<? super T, ? extends K> keyMapper,
				Supplier<Map<K, T>> supplier) {
			this.keyMapper = keyMapper;
			this.supplier = supplier;
		}

		@Override
		public BiConsumer<Map<K, T>, T> accumulator() {
			return (map, value) -> map.put(keyMapper.apply(value), value);
		}

		@Override
		public Set<java.util.stream.Collector.Characteristics>
				characteristics() {
			return EnumSet.of(Characteristics.UNORDERED,
					Characteristics.IDENTITY_FINISH);
		}

		@Override
		public BinaryOperator<Map<K, T>> combiner() {
			return (left, right) -> {
				left.putAll(right);
				return left;
			};
		}

		@Override
		public Function<Map<K, T>, Map<K, T>> finisher() {
			return null;
		}

		@Override
		public Supplier<Map<K, T>> supplier() {
			return supplier;
		}
	}

	private static class ToMultimapCollector<T, K, U> implements
			java.util.stream.Collector<T, Multimap<K, List<U>>, Multimap<K, List<U>>> {
		private Function<? super T, ? extends K> keyMapper;

		private Function<? super T, ? extends U> valueMapper;

		private Supplier<Multimap<K, List<U>>> supplier;

		public ToMultimapCollector(Function<? super T, ? extends K> keyMapper,
				Function<? super T, ? extends U> valueMapper,
				Supplier<Multimap<K, List<U>>> supplier) {
			this.keyMapper = keyMapper;
			this.valueMapper = valueMapper;
			this.supplier = supplier;
		}

		@Override
		public BiConsumer<Multimap<K, List<U>>, T> accumulator() {
			return (map, value) -> map.add(keyMapper.apply(value),
					valueMapper.apply(value));
		}

		@Override
		public Set<java.util.stream.Collector.Characteristics>
				characteristics() {
			return EnumSet.of(Characteristics.UNORDERED,
					Characteristics.IDENTITY_FINISH);
		}

		@Override
		public BinaryOperator<Multimap<K, List<U>>> combiner() {
			return (left, right) -> {
				left.addAll(right);
				return left;
			};
		}

		@Override
		public Function<Multimap<K, List<U>>, Multimap<K, List<U>>> finisher() {
			return null;
		}

		@Override
		public Supplier<Multimap<K, List<U>>> supplier() {
			return supplier;
		}
	}

	private static class ToUnsortedMultikeyMapCollector<T, V> implements
			java.util.stream.Collector<T, UnsortedMultikeyMap<V>, UnsortedMultikeyMap<V>> {
		private Function<? super T, Object[]> mapper;

		private Supplier<UnsortedMultikeyMap<V>> supplier;

		public ToUnsortedMultikeyMapCollector(
				Function<? super T, Object[]> mapper, int depth) {
			this.mapper = mapper;
			this.supplier = new Supplier<UnsortedMultikeyMap<V>>() {
				@Override
				public UnsortedMultikeyMap<V> get() {
					return new UnsortedMultikeyMap<V>(depth);
				}
			};
		}

		@Override
		public BiConsumer<UnsortedMultikeyMap<V>, T> accumulator() {
			return (map, value) -> map.put(mapper.apply(value));
		}

		@Override
		public Set<java.util.stream.Collector.Characteristics>
				characteristics() {
			return EnumSet.of(Characteristics.UNORDERED,
					Characteristics.IDENTITY_FINISH);
		}

		@Override
		public BinaryOperator<UnsortedMultikeyMap<V>> combiner() {
			return (left, right) -> {
				left.putMulti(right);
				return left;
			};
		}

		@Override
		public Function<UnsortedMultikeyMap<V>, UnsortedMultikeyMap<V>>
				finisher() {
			return null;
		}

		@Override
		public Supplier<UnsortedMultikeyMap<V>> supplier() {
			return supplier;
		}
	}

	public static <T> Collector<T, ?, Set<T>> toLiSet() {
		return new ToLiSetCollector<>();
	}
}
