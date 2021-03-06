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
package cc.alcina.framework.gwt.client.gwittir.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.ui.table.HasChunks;
import com.totsp.gwittir.client.ui.table.SortableDataProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

/**
 *
 * @author Nick Reddel
 */
public class CollectionDataProvider implements SortableDataProvider {
	private int pageSize = 50;

	ArrayList sort;

	public CollectionDataProvider(Collection c) {
		sort = new ArrayList(c);
	}

	public void getChunk(final HasChunks table, final int chunkNumber) {
		table.setChunk(getChunk(chunkNumber));
	}

	public <V> Collection<? extends V> getChunk(int chunkNumber) {
		ArrayList result = new ArrayList();
		int maxSize = Math.min(getPageSize(),
				sort.size() - chunkNumber * getPageSize());
		for (int i = 0; i < maxSize; i++) {
			result.add(sort.get(i + chunkNumber * getPageSize()));
		}
		return result;
	}

	public int getPageSize() {
		return pageSize;
	}

	public String[] getSortableProperties() {
		Object first = CommonUtils.first(sort);
		if (first == null) {
			return new String[0];
		}
		ClientBeanReflector bi = ClientReflector.get()
				.beanInfoForClass(first.getClass());
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		List<String> fieldNames = new ArrayList<String>();
		for (ClientPropertyReflector pr : prs) {
			Property p = GwittirBridge.get().getProperty(first,
					pr.getPropertyName());
			try {
				Object o = p.getAccessorMethod().invoke(first,
						CommonUtils.EMPTY_OBJECT_ARRAY);
				if (o instanceof Collection) {
					// continue; //in fact, catch this in sortOnProperty
					// (easier, just ignore)
				}
				fieldNames.add(pr.getPropertyName());
			} catch (Exception e) {
			}
		}
		return (String[]) fieldNames.toArray(new String[fieldNames.size()]);
	}

	public void init(final HasChunks table) {
		table.init(getChunk(0),
				(sort.size() - 1) / Math.max(getPageSize(), 1) + 1);
	}

	public void putCollection(Collection c, HasChunks table) {
		sort = new ArrayList(c);
		if (table != null) {
			init(table);
		}
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public void showAllObjectsInCollection() {
		setPageSize(sort.size());
	}

	public void sortOnProperty(final HasChunks table, String propertyName,
			boolean ascending) {
		try {
			if (!sort.isEmpty()) {
				Object o = sort.get(0);
				Property p = GwittirBridge.get().getProperty(o, propertyName);
				Map<Object, List<Object>> cMap = new HashMap<Object, List<Object>>();
				for (Object o2 : sort) {
					Object pVal = p.getAccessorMethod().invoke(o2,
							CommonUtils.EMPTY_OBJECT_ARRAY);
					if (!cMap.containsKey(pVal)) {
						cMap.put(pVal, new ArrayList<Object>());
					}
					cMap.get(pVal).add(o2);
				}
				List<Object> nList = cMap.get(null);
				cMap.remove(null);
				ArrayList keys = new ArrayList(cMap.keySet());
				Comparator comparator = getComparator(propertyName, keys);
				if (comparator != null) {
					Collections.sort(keys, comparator);
				} else {
					Collections.sort(keys);
				}
				if (!ascending) {
					Collections.reverse(keys);
				}
				sort = new ArrayList();
				for (Object object : keys) {
					sort.addAll(cMap.get(object));
				}
				if (nList != null) {
					sort.addAll(ascending ? 0 : Math.max(0, sort.size() - 1),
							nList);
				}
			}
		} catch (Exception e) {
			return;
			// non sortable column
		}
		// allow for pre-attach sorting
		if (table != null) {
			init(table);
		}
	}

	protected Comparator getComparator(String propertyName, ArrayList keys) {
		if (!keys.isEmpty()) {
			Object key = keys.get(0);
			if (key instanceof String) {
				return String.CASE_INSENSITIVE_ORDER;
			}
		}
		return null;
	}
}
