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
package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import cc.alcina.framework.common.client.collections.ImmutableMap;

import com.totsp.gwittir.client.beans.Converter;

/**
 * chains of lookups - depth does not include the looked-up object: e.g.
 * class/id/instance would be depth 2
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
public class SortedMultikeyMap<V> extends MultikeyMapBase<V> {
	
	@Override
	public MultikeyMap<V> createMap(int childDepth) {
		return new SortedMultikeyMap(childDepth);
	}
	@Override
	public <T> Collection<T> reverseKeys(Object... objects) {
		TreeMap m = (TreeMap) asMapEnsureDelegate(false, objects);
		return m == null ? null : m.descendingMap().keySet();
	}

	@Override
	public <T> Collection<T> reverseValues(Object... objects) {
		TreeMap m = (TreeMap) asMapEnsureDelegate(false, objects);
		return m == null ? null : m.descendingMap().values();
	}
	public SortedMultikeyMap() {
		this(2);
	}

	public SortedMultikeyMap(int depth) {
		this.depth = depth;
		this.delegate = new TreeMap();
	}

}
