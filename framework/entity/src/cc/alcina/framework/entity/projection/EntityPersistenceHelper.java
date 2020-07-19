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
import java.util.List;
import java.util.TreeMap;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.entity.SEUtilities;

/**
 *
 * @author Nick Reddel
 */
public class EntityPersistenceHelper {
	public static <T extends HasId> void order(List<T> incoming,
			List<Long> orderValues) {
		final TreeMap<Long, Integer> orderPositions = new TreeMap<Long, Integer>();
		for (int i = 0; i < orderValues.size(); i++) {
			Long l = orderValues.get(i);
			orderPositions.put(l, i);
		}
		Comparator<T> cmp = new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return orderPositions.get(o1.getId())
						.compareTo(orderPositions.get(o2.getId()));
			}
		};
		Collections.sort(incoming, cmp);
	}

	public static List<Long> toIdList(Collection<? extends HasId> hasIds) {
		return toIdList(hasIds, false);
	}

	public static List<Long> toIdList(Collection<? extends HasId> hasIds,
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

	public static String toInClause(Collection<?> objs) {
		StringBuffer sb = new StringBuffer();
		sb.append(" (-1");
		for (Object obj : objs) {
			if (obj == null) {
				continue;
			}
			sb.append(", ");
			if (obj instanceof Long) {
				sb.append(obj);
			} else if (obj instanceof HasId) {
				sb.append(((HasId) obj).getId());
			} else {
				throw new IllegalArgumentException();
			}
		}
		sb.append(") ");
		return sb.toString();
	}

	public static String toInLongsClause(Long[] longs) {
		return toInClause(Arrays.asList(longs));
	}

	public static String toInStringsClause(Collection<String> strs) {
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
}
