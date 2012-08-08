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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.HasId;

/**
 * 
 * @author Nick Reddel
 */
public class DefaultCollectionFilter {
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

	public static <V> void filterInPlace(Collection<? extends V> collection,
			CollectionFilter<V> filter) {
		for (Iterator<V> itr = (Iterator<V>) collection.iterator(); itr
				.hasNext();) {
			if (!filter.allow(itr.next())) {
				itr.remove();
			}
		}
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

	public static <I, O> List<O> filterByClass(
			Collection<? extends I> collection, Class<? extends O> filterClass) {
		ArrayList<O> result = new ArrayList<O>();
		for (I i : collection) {
			if (i.getClass() == filterClass) {
				result.add((O) i);
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

	public static <V> V singleNodeFilter(Collection<? extends V> collection,
			CollectionFilter<V> filter) {
		for (V v : collection) {
			if (filter.allow(v)) {
				return v;
			}
		}
		return null;
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
			Collection<? extends T> collection, ConverterFilter<T, C> converterFilter) {
		List<C> result = new ArrayList<C>();
		for (T t : collection) {
			if(!converterFilter.allowPreConvert(t)){
				continue;
			}
			C convert = converterFilter.convert(t);
			if (converterFilter.allowPostConvert(convert)) {
				result.add(convert);
			}
		}
		return result;
	}

	public static final CollectionFilter PASSTHROUGH_FILTER = new CollectionFilter() {
		@Override
		public boolean allow(Object o) {
			return true;
		}
	};

	public static interface ConverterFilter<T, C> extends Converter<T, C>
			 {
		public boolean allowPreConvert(T t);
		public boolean allowPostConvert(C c);
	}
}