package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
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
import java.util.stream.Collector.Characteristics;

import com.google.gwt.core.client.GwtScriptOnly;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Multimap;

@GwtScriptOnly
public class AlcinaCollectorsGwt extends AlcinaCollectors {
	private static class ToItemStreamCollector<T>
			implements java.util.stream.Collector<Collection<T>, T, Stream<T>> {
		public Stream<T> collect(Stream<Collection<T>> stream) {
			List<T> result = new ArrayList<T>();
			for (Iterator<Collection<T>> itr = stream.iterator(); itr
					.hasNext();) {
				result.addAll(itr.next());
			}
			return result.stream();
		}
	}

	public <T> Collector<Collection<T>, ?, Stream<T>> toItemStream0() {
		return new ToItemStreamCollector();
	}

	public <T, K, U> Collector<T, ?, Multimap<K, List<U>>>
			toKeyMultimap0(Function<? super T, ? extends K> keyMapper) {
		return (Collector) toMultimap(keyMapper, t -> t);
	}

	public  <T, K> Collector<T, ?, Map<K, T>>
			toKeyMap0(Function<? super T, ? extends K> keyMapper) {
		return new ToMapCollector(keyMapper, LinkedHashMap::new);
	}

	public <T, K, U> Collector<T, ?, Multimap<K, List<U>>> toMultimap0(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return new ToMultimapCollector(keyMapper, valueMapper, Multimap::new);
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

		public Multimap<K, List<U>> collect(Stream<T> stream) {
			Multimap<K, List<U>> result = supplier.get();
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				T next = itr.next();
				result.add(keyMapper.apply(next), valueMapper.apply(next));
			}
			return result;
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

		public Map<K, T> collect(Stream<T> stream) {
			Map<K, T> result = supplier.get();
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				T next = itr.next();
				result.put(keyMapper.apply(next), next);
			}
			return result;
		}
	}
}