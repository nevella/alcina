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
package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.entity.entityaccess.DetachedEntityCache;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;
import cc.alcina.framework.entity.util.GraphProjection.PermissibleFieldFilter;

/**
 * 
 * @author Nick Reddel
 */
public class EntityUtils {
	public static String longListToIdClause(Long[] longs) {
		return longListToIdClause(Arrays.asList(longs));
	}

	public static String hasIdListToIdClause(Collection<? extends HasId> hasIds) {
		return longListToIdClause(hasIdListToIdList(hasIds));
	}

	public static List<Long> hasIdListToIdList(
			Collection<? extends HasId> hasIds) {
		List<Long> ids = new ArrayList<Long>();
		for (HasId hasId : hasIds) {
			ids.add(hasId.getId());
		}
		return ids;
	}

	public static List<Long> idClauseToLongArray(String str) {
		ArrayList<Long> result = new ArrayList<Long>();
		String[] strs = str.replace("(", "").replace(")", "").split(",\\s*");
		for (String s : strs) {
			String t = s.trim();
			if (t.length() > 0) {
				long value = Long.parseLong(t);
				if (value > 0) {
					result.add(value);
				}
			}
		}
		return result;
	}

	public static String longListToIdClause(Collection<Long> longs) {
		StringBuffer sb = new StringBuffer();
		sb.append(" (-1");
		for (Long long1 : longs) {
			sb.append(", ");
			sb.append(long1);
		}
		sb.append(") ");
		return sb.toString();
	}

	public static String unbracketedIdClause(Collection<Long> longs) {
		StringBuffer sb = new StringBuffer();
		for (Long long1 : longs) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(long1);
		}
		return sb.toString();
	}

	public static String stringListToClause(Collection<String> strs) {
		StringBuffer sb = new StringBuffer();
		sb.append(" (");
		for (String str : strs) {
			sb.append(sb.length() == 2 ? "'" : ", '");
			sb.append(str.replace("'", "''"));
			sb.append("'");
		}
		sb.append(") ");
		return sb.toString();
	}

	public static void checkDbIdentifier(String s) {
		String regex = "\\w+";
		if (!s.matches(regex)) {
			throw new RuntimeException("Injection exception");
		}
	}

	public <T> T detachedClone(T source, InstantiateImplCallback callback) {
		return detachedClone(source, false, callback);
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

	public <T> T detachedClone(T source, InstantiateImplCallback callback,
			DetachedEntityCache cache) {
		GraphProjectionFilter filter = EntityLayerLocator.get()
				.jpaImplementation().getResolvingFilter(callback, cache);
		try {
			return new GraphProjection(new PermissibleFieldFilter(), filter)
					.project(source, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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
				return orderPositions.get(o1.getId()).compareTo(
						orderPositions.get(o2.getId()));
			}
		};
		Collections.sort(incoming, cmp);
	}
}
