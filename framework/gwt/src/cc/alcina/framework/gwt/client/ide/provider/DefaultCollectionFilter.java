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

package cc.alcina.framework.gwt.client.ide.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
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