package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
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
import java.util.stream.Stream;

import com.google.gwt.core.client.GwtScriptOnly;

@GwtScriptOnly
public class AlcinaCollectorsGwt extends AlcinaCollectors {
	@Override
	public <T> Collector<Collection<T>, ?, Stream<T>> toItemStream0() {
		return new ToItemStreamCollector();
	}

	@Override
	public <T, K> Collector<T, ?, Map<K, T>>
			toKeyMap0(Function<? super T, ? extends K> keyMapper) {
		return new ToMapCollector(keyMapper, LinkedHashMap::new);
	}

	@Override
	public <T, K, U> Collector<T, ?, Multimap<K, List<U>>>
			toKeyMultimap0(Function<? super T, ? extends K> keyMapper) {
		return (Collector) toMultimap(keyMapper, t -> t);
	}

	@Override
	public <T, K, U> Collector<T, ?, Multimap<K, List<U>>> toMultimap0(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return new ToMultimapCollector(keyMapper, valueMapper, Multimap::new);
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
}