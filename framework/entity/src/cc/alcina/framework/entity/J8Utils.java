package cc.alcina.framework.entity;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import cc.alcina.framework.common.client.util.Multimap;

public class J8Utils {
	public static <T, K> Collector<T, ?, Map<K, T>> toKeyMap(
			Function<? super T, ? extends K> keyMapper) {
		return new ToMapCollector(keyMapper, LinkedHashMap::new);
	}

	public static <T, K, U> Collector<T, ?, Multimap<K, List<U>>> toMultimap(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return new ToMultimapCollector(keyMapper, valueMapper, Multimap::new);
	}

	private static class ToMapCollector<T, K> implements
			java.util.stream.Collector<T, Map<K, T>, Map<K, T>> {
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
		public Set<java.util.stream.Collector.Characteristics> characteristics() {
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
		public Supplier<Map<K, T>> supplier() {
			return supplier;
		}

		@Override
		public Function<Map<K, T>, Map<K, T>> finisher() {
			return null;
		}
	}

	private static class ToMultimapCollector<T, K, U>
			implements
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
		public Set<java.util.stream.Collector.Characteristics> characteristics() {
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
}
