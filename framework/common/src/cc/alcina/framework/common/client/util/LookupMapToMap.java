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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * chains of lookups - depth does not include the looked-up object: e.g.
 * class/id/instance would be depth 2
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
public class LookupMapToMap<V> extends LinkedHashMap {
	private final int depth;

	public LookupMapToMap(int depth) {
		this.depth = depth;
	}

	public void put(Object... objects) {
		Map m = this;
		int mapDepth = depth;
		for (int i = 0; i < objects.length; i++) {
			Object k = objects[i];
			if (--mapDepth == 0) {
				m.put(k, objects[i + 1]);
				return;
			}
			if (!m.containsKey(k)) {
				m.put(k, new LookupMapToMap(depth - 1));
			}
			m = (Map) m.get(k);
		}
	}

	public boolean containsKey(Object... objects) {
		Map m = this;
		int mapDepth = depth;
		for (int i = 0; i < objects.length; i++) {
			Object k = objects[i];
			if (--mapDepth == 0) {
				return m.containsKey(k);
			}
			if (!m.containsKey(k)) {
				m.put(k, new LookupMapToMap(depth - 1));
			}
			m = (Map) m.get(k);
		}
		return false;
	}

	public Map asMap(Object... objects) {
		Map m = this;
		int mapDepth = depth;
		for (int i = 0; i < objects.length; i++) {
			Object k = objects[i];
			if (!m.containsKey(k)) {
				m.put(k, new LookupMapToMap(depth - 1));
			}
			m = (Map) m.get(k);
			if (i == objects.length - 1) {
				return m;
			}
		}
		return null;
	}

	public <T> Collection<T> values(Object... objects) {
		Map m = asMap(objects);
		return m == null ? null : m.values();
	}

	public V get(Object... objects) {
		Map m = this;
		int mapDepth = depth;
		for (int i = 0; i < objects.length; i++) {
			Object k = objects[i];
			if (--mapDepth == 0) {
				return (V) m.get(k);
			}
			if (!m.containsKey(k)) {
				m.put(k, new LookupMapToMap(depth - 1));
			}
			m = (Map) m.get(k);
		}
		return null;
	}
}
