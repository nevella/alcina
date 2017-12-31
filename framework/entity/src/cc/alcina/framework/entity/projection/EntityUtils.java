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
package cc.alcina.framework.entity.projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.TreeMap;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjection.InstantiateImplCallback;

/**
 *
 * @author Nick Reddel
 */
public class EntityUtils {
	public static void checkDbIdentifier(String s) {
		String regex = "\\w+";
		if (!s.matches(regex)) {
			throw new RuntimeException("Injection exception");
		}
	}

	public static String hasIdsToIdClause(Collection<? extends HasId> hasIds) {
		return longsToIdClause(hasIdsToIdList(hasIds));
	}

	public static List<Long>
			hasIdsToIdList(Collection<? extends HasId> hasIds) {
		return hasIdsToIdList(hasIds, false);
	}

	public static List<Long> hasIdsToIdList(Collection<? extends HasId> hasIds,
			boolean withMinusOne) {
		List<Long> ids = new ArrayList<Long>();
		if (withMinusOne) {
			ids.add(-1L);
		}
		for (HasId hasId : hasIds) {
			ids.add(hasId.getId());
		}
		return ids;
	}

	public static String longArrayToIdClause(Long[] longs) {
		return longsToIdClause(Arrays.asList(longs));
	}

	public static String longsToIdClause(Collection<Long> longs) {
		StringBuffer sb = new StringBuffer();
		sb.append(" (-1");
		for (Long long1 : longs) {
			sb.append(", ");
			sb.append(long1);
		}
		sb.append(") ");
		return sb.toString();
	}

	public static <T extends HasId> void order(List<T> incoming,
			List<Long> orderValues) {
		final TreeMap<Long, Integer> orderPositions = new TreeMap<Long, Integer>();
		for (int i = 0; i < orderValues.size(); i++) {
			Long l = orderValues.get(i);
			orderPositions.put(l, i);
		}
		Comparator<T> cmp = new Comparator<T>() {
			public int compare(T o1, T o2) {
				return orderPositions.get(o1.getId())
						.compareTo(orderPositions.get(o2.getId()));
			}
		};
		Collections.sort(incoming, cmp);
	}

	public static String stringListToClause(Collection<String> strs) {
		StringBuffer sb = new StringBuffer();
		if (strs.isEmpty()) {
			strs.add("value##never##matched--" + SEUtilities.generateId());
		}
		sb.append(" (");
		for (String str : strs) {
			sb.append(sb.length() == 2 ? "'" : ", '");
			sb.append(str.replace("'", "''"));
			sb.append("'");
		}
		sb.append(") ");
		return sb.toString();
	}

	public <T> T detachedClone(T source) {
		return detachedClone(source, false, null);
	}

	public <T> T detachedClone(T source, boolean useCache) {
		return detachedClone(source, useCache, null);
	}

	public <T> T detachedClone(T source, boolean useCache,
			InstantiateImplCallback callback) {
		DetachedEntityCache cache = useCache ? null : new DetachedEntityCache();
		return detachedClone(source, callback, cache);
	}

	public <T> T detachedClone(T source, InstantiateImplCallback callback) {
		return detachedClone(source, false, callback);
	}

	public <T> T detachedClone(T source, InstantiateImplCallback callback,
			DetachedEntityCache cache) {
		return detachedClone(source, callback, cache, false);
	}

	public <T> T detachedClone(T source, InstantiateImplCallback callback,
			DetachedEntityCache cache, boolean useMemCache) {
		try {
			return projections(callback, cache, useMemCache).project(source);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <T> T detachedCloneIgnorePermissions(T source,
			InstantiateImplCallback callback) {
		DetachedEntityCache cache = new DetachedEntityCache();
		GraphProjectionDataFilter dataFilter = Registry
				.impl(JPAImplementation.class)
				.getResolvingFilter(callback, cache, false);
		try {
			return new GraphProjection(null, dataFilter).project(source, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <T> T detachedCloneWithMemCache(T source,
			InstantiateImplCallback callback) {
		return detachedClone(source, callback, new DetachedEntityCache(), true);
	}

	public GraphProjections projections(InstantiateImplCallback callback,
			DetachedEntityCache cache, boolean useMemCache) {
		GraphProjectionDataFilter dataFilter = Registry
				.impl(JPAImplementation.class)
				.getResolvingFilter(callback, cache, useMemCache);
		return GraphProjections.defaultProjections()
				.fieldFilter(Registry.impl(PermissibleFieldFilter.class))
				.dataFilter(dataFilter);
	}

	static class MultiIdentityMap
			extends IdentityHashMap<Object, IdentityHashMap<Object, Object>> {
		public void add(Object o1, Object o2) {
			ensureKey(o1);
			get(o1).put(o2, o2);
		}

		public void ensureKey(Object o1) {
			if (!containsKey(o1)) {
				put(o1, new IdentityHashMap<Object, Object>());
			}
		}
	}
}
