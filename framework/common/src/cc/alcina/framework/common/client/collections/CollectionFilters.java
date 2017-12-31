/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;

/**
 *
 * @author Nick Reddel
 */
public class CollectionFilters {
	public static final CollectionFilter PASSTHROUGH_FILTER = new CollectionFilter() {
		@Override
		public boolean allow(Object o) {
			return true;
		}
	};

	public static final CollectionFilter NO_FILTER = new CollectionFilter() {
		@Override
		public boolean allow(Object o) {
			return false;
		}
	};

	public static final Converter TO_NULL_CONVERTER = new Converter() {
		@Override
		public Object convert(Object original) {
			return null;
		}
	};

	public static final CollectionFilter<String> IS_NOT_NULL_OR_EMPTY_FILTER = new CollectionFilter<String>() {
		@Override
		public boolean allow(String o) {
			return CommonUtils.isNotNullOrEmpty(o);
		}
	};

	public static <V> void apply(Collection<? extends V> collection,
			Callback<V> callback) {
		for (Iterator<V> itr = (Iterator<V>) collection.iterator(); itr
				.hasNext();) {
			callback.apply(itr.next());
		}
	}

	public static <T extends PublicCloneable<T>> List<T> clone(List<T> source) {
		return convert(source, new CloneProjector<T>());
	}

	public static <V> boolean contains(Collection<? extends V> collection,
			CollectionFilter<V> filter) {
		for (V v : collection) {
			if (filter.allow(v)) {
				return true;
			}
		}
		return false;
	}

	public static <T, C> List<C> convert(Collection<? extends T> collection,
			Converter<T, C> converter) {
		List<C> result = new ArrayList<C>();
		for (T t : collection) {
			result.add(converter.convert(t));
		}
		return result;
	}

	public static <T, C> List<C> convertAndFilter(
			Collection<? extends T> collection,
			ConverterFilter<T, C> converterFilter) {
		return convertAndFilter(collection, converterFilter, 0);
	}

	public static <T, C> List<C> convertAndFilter(
			Collection<? extends T> collection,
			ConverterFilter<T, C> converterFilter, int limit) {
		List<C> result = new ArrayList<C>();
		for (T t : collection) {
			if (!converterFilter.allowPreConvert(t)) {
				continue;
			}
			C convert = converterFilter.convert(t);
			if (converterFilter.allowPostConvert(convert)) {
				result.add(convert);
				if (--limit == 0) {
					break;
				}
			}
		}
		return result;
	}

	public static <T, C> Set<C> distinct(Collection<? extends T> collection,
			Converter<T, C> converter) {
		return new LinkedHashSet<C>(convert(collection, converter));
	}

	public static <V> List<V> filter(Collection<? extends V> collection,
			CollectionFilter<V> filter) {
		ArrayList<V> result = new ArrayList<V>();
		for (V v : collection) {
			if (filter.allow(v)) {
				result.add(v);
			}
		}
		return result;
	}

	public static <V> Set<V> filterAsSet(Collection<? extends V> collection,
			CollectionFilter<V> filter) {
		LinkedHashSet<V> result = new LinkedHashSet<V>();
		for (V v : collection) {
			if (filter.allow(v)) {
				result.add(v);
			}
		}
		return result;
	}

	public static <I, O> List<O> filterByClass(
			Collection<? extends I> collection,
			Class<? extends O> filterClass) {
		ArrayList<O> result = new ArrayList<O>();
		for (I i : collection) {
			if (i.getClass() == filterClass) {
				result.add((O) i);
			}
		}
		return result;
	}

	public static <V extends HasId> List<V> filterByIds(
			Collection<? extends V> collection, Collection<Long> ids) {
		ArrayList<V> result = new ArrayList<V>();
		for (V v : collection) {
			if (ids.contains(v.getId())) {
				result.add(v);
			}
		}
		return result;
	}

	public static <V> List<V> filterByProperty(
			Collection<? extends V> collection, String key, Object value) {
		return filter(collection, new PropertyFilter(key, value));
	}

	public static <V> void filterInPlace(Collection<? extends V> collection,
			CollectionFilter<V> filter) {
		for (Iterator<V> itr = (Iterator<V>) collection.iterator(); itr
				.hasNext();) {
			if (!filter.allow(itr.next())) {
				itr.remove();
			}
		}
	}

	public static void filterInPlaceByProperty(Collection collection,
			String key, Object value) {
		filterInPlace(collection, new PropertyFilter(key, value));
	}

	public static <V> Collection<V> filterOrReturn(Collection<V> collection,
			CollectionFilter<V> filter) {
		if (contains(collection, filter)) {
			return filter(collection, filter);
		}
		return collection;
	}

	public static <V> V first(Collection<V> values,
			CollectionFilter<V> filter) {
		for (Iterator<V> itr = values.iterator(); itr.hasNext();) {
			V v = itr.next();
			if (filter.allow(v)) {
				return v;
			}
		}
		return null;
	};

	public static <V> V first(Collection<V> values, String key, Object value) {
		PropertyFilter<V> filter = new PropertyFilter<V>(key, value);
		return first(values, filter);
	}

	public static <V> V firstOfClass(Collection values,
			Class<? extends V> clazz) {
		IsClassFilter filter = new IsClassFilter(clazz);
		return (V) first(values, filter);
	}

	public static <V> int indexOf(Collection<? extends V> collection,
			CollectionFilter<V> filter) {
		int i = 0;
		for (V v : collection) {
			if (filter.allow(v)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public static final <CF extends CollectionFilter> CF
			inverse(final CF filter) {
		return (CF) new CollectionFilter() {
			@Override
			public boolean allow(Object o) {
				return !filter.allow(o);
			}
		};
	}

	public static <K, V> Map<K, V> invert(Map<V, K> map) {
		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Entry<V, K> entry : map.entrySet()) {
			result.put(entry.getValue(), entry.getKey());
		}
		return result;
	}

	public static <T> List<T> limit(Collection<T> values, int count) {
		List<T> result = new ArrayList<T>();
		for (T t : values) {
			if (count-- == 0) {
				break;
			}
			result.add(t);
		}
		return result;
	}

	public static <K, V, O> Map<K, V> map(Collection<O> values,
			KeyValueMapper<K, V, O> mapper) {
		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Iterator<O> itr = values.iterator(); itr.hasNext();) {
			O o = itr.next();
			if (mapper instanceof CollectionFilter
					&& !((CollectionFilter) mapper).allow(o)) {
				continue;
			}
			result.put(mapper.getKey(o), mapper.getValue(o));
		}
		return result;
	}

	public static <T extends Comparable<T>> T max(Collection<T> collection) {
		return max(collection, null);
	}

	public static <T> T max(Collection<T> collection,
			Comparator<T> comparator) {
		T max = null;
		for (T t : collection) {
			if (max == null || (comparator != null ? comparator.compare(max, t)
					: ((Comparable) max).compareTo((Comparable) t)) < 0) {
				max = t;
			}
		}
		return max;
	}

	public static <T extends Comparable<T>> T min(Collection<T> collection) {
		return min(collection, null);
	}

	public static <T> T min(Collection<T> collection,
			Comparator<T> comparator) {
		T min = null;
		for (T t : collection) {
			if (min == null || (comparator != null ? comparator.compare(min, t)
					: ((Comparable) min).compareTo((Comparable) t)) > 0) {
				min = t;
			}
		}
		return min;
	}

	public static <K, V, O> Multimap<K, List<V>> multimap(Collection<O> values,
			KeyValueMapper<K, V, O> mapper) {
		Multimap<K, List<V>> result = new Multimap<K, List<V>>();
		for (Iterator<O> itr = values.iterator(); itr.hasNext();) {
			O o = itr.next();
			result.add(mapper.getKey(o), mapper.getValue(o));
		}
		return result;
	}

	public static <V, T> V project(Collection<T> values,
			CollectionProjector<T, V> projector) {
		for (T t : values) {
			projector.tryProject(t);
		}
		return projector.getBestValue();
	}

	public static <T> List<T> propertyList(Collection values,
			String propertyPath) {
		return convert(values, new PropertyConverter<Object, T>(propertyPath));
	}

	public static <V> V singleNodeFilter(Collection<? extends V> collection,
			CollectionFilter<V> filter) {
		for (V v : collection) {
			if (filter.allow(v)) {
				return v;
			}
		}
		return null;
	}

	public static <K, V, O> SortedMap<K, V> sortedMap(Collection<O> values,
			KeyValueMapper<K, V, O> mapper) {
		SortedMap<K, V> result = new TreeMap<K, V>();
		for (Iterator<O> itr = values.iterator(); itr.hasNext();) {
			O o = itr.next();
			if (mapper instanceof CollectionFilter
					&& !((CollectionFilter) mapper).allow(o)) {
				continue;
			}
			result.put(mapper.getKey(o), mapper.getValue(o));
		}
		return result;
	}

	public static <K, V1, V2> Map<K, V2> transformMap(Map<K, V1> mapIn,
			Converter<V1, V2> converter) {
		Map<K, V2> result = new LinkedHashMap<K, V2>();
		Set<Entry<K, V1>> entrySet = mapIn.entrySet();
		for (Entry<K, V1> entry : entrySet) {
			result.put(entry.getKey(), converter.convert(entry.getValue()));
		}
		return result;
	}

	public static class ContainsFilter<T> implements CollectionFilter<T> {
		private Collection<T> collection;

		public ContainsFilter(Collection<T> collection) {
			this.collection = collection;
		}

		@Override
		public boolean allow(T t) {
			return collection.contains(t);
		}
	}

	public static interface ConverterFilter<T, C> extends Converter<T, C> {
		public boolean allowPostConvert(C c);

		public boolean allowPreConvert(T t);
	}

	public static final class InverseFilter implements CollectionFilter {
		private final CollectionFilter invert;

		public InverseFilter(CollectionFilter invert) {
			this.invert = invert;
		}

		@Override
		public boolean allow(Object o) {
			return !invert.allow(o);
		}

		@Override
		public void setContext(FilterContext context) {
			invert.setContext(context);
		}

		@Override
		public String toString() {
			return "NOT (" + invert + ")";
		}
	}

	public static class PrefixedFilter implements CollectionFilter<String> {
		private String lcPrefix;

		public PrefixedFilter(String prefix) {
			this.lcPrefix = prefix.toLowerCase();
		}

		@Override
		public boolean allow(String o) {
			return CommonUtils.nullToEmpty(o).toLowerCase()
					.startsWith(lcPrefix);
		}
	}

	public static class ToLongConverter implements Converter<String, Long> {
		@Override
		public Long convert(String o) {
			return o == null ? null : Long.parseLong(o);
		}
	}
}
