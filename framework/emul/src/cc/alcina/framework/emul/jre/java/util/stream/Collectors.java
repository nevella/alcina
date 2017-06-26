package java.util.stream;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.function.ToIntFunction;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;

import cc.alcina.framework.entity.J8Utils.ToItemStreamCollector;

public class Collectors {
	private static class ToListCollector<T>
			implements java.util.stream.Collector<T, T, List<T>> {
		public List<T> collect(Stream<T> stream) {
			List<T> result = new ArrayList<T>();
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				result.add(itr.next());
			}
			return result;
		}
	}

	private static class ToSetCollector<T>
			implements java.util.stream.Collector<T, T, Set<T>> {
		public Set<T> collect(Stream<T> stream) {
			Set<T> result = new LinkedHashSet<T>();
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				result.add(itr.next());
			}
			return result;
		}
	}

	private static class JoiningCollector<T>
			implements java.util.stream.Collector<T, T, String> {
		private CharSequence separator;

		public JoiningCollector(CharSequence separator) {
			this.separator = separator;
		}

		public String collect(Stream<T> stream) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				if (first) {
					first = false;
				} else {
					sb.append(separator);
				}
				sb.append(itr.next().toString());
			}
			return sb.toString();
		}
	}

	private static class ToIntCollector<T>
			implements java.util.stream.Collector<T, T, Integer> {
		ToIntFunction<? super T> mapper;

		ToIntCollector(ToIntFunction<? super T> mapper) {
			this.mapper = mapper;
		}

		public Integer collect(Stream<T> stream) {
			int result = 0;
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				result += mapper.applyAsInt(itr.next());
			}
			return result;
		}
	}

	private static class ToDoubleCollector<T>
			implements java.util.stream.Collector<T, T, Double> {
		ToDoubleFunction<? super T> mapper;

		ToDoubleCollector(ToDoubleFunction<? super T> mapper) {
			this.mapper = mapper;
		}

		public Double collect(Stream<T> stream) {
			double result = 0;
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				result += mapper.applyAsDouble(itr.next());
			}
			return result;
		}
	}

	private static class ToMapCollector<T, K, U>
			implements java.util.stream.Collector<T, T, Map<K, U>> {
		private Function<? super T, ? extends K> keyMapper;

		private Function<? super T, ? extends U> valueMapper;

		public ToMapCollector(Function<? super T, ? extends K> keyMapper,
				Function<? super T, ? extends U> valueMapper) {
			this.keyMapper = keyMapper;
			this.valueMapper = valueMapper;
		}

		public Map<K, U> collect(Stream<T> stream) {
			Map<K, U> result = new LinkedHashMap<>();
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				T t = itr.next();
				result.put(keyMapper.apply(t), valueMapper.apply(t));
			}
			return result;
		}
	}

	public static <T> Collector<T, ?, List<T>> toList() {
		return new ToListCollector<T>();
	}

	public static <T> Collector<T, ?, Set<T>> toSet() {
		return new ToSetCollector<T>();
	}

	public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return new ToMapCollector(keyMapper, valueMapper);
	}

	public static <T> Collector<T, T, String> joining() {
		return new JoiningCollector<T>("");
	}

	public static <T> Collector<T, ?, Integer>
			summingInt(ToIntFunction<? super T> mapper) {
		return new ToIntCollector(mapper);
	}

	public static <T> Collector<T, ?, Double>
			summingDouble(ToDoubleFunction<? super T> mapper) {
		return new ToDoubleCollector(mapper);
	}

	public static <T> Collector<T, T, String> joining(CharSequence separator) {
		return new JoiningCollector<T>(separator);
	}
}
