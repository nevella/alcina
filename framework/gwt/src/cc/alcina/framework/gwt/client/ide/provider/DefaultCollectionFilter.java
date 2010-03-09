/**
 * 
 */
package cc.alcina.framework.gwt.client.ide.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

	public static <V> V singleNodeFilter(Collection<? extends V> collection,
			CollectionFilter<V> filter) {
		for (V v : collection) {
			if (filter.allow(v)) {
				return v;
			}
		}
		return null;
	}
}