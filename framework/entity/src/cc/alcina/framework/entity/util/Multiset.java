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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Here because GWT RPC doesn't like this to be client-viz
 * 
 * @author nick@alcina.cc
 * 
 * @param <K>
 * @param <V>
 */
@SuppressWarnings("unchecked")
public class Multiset<K, V extends Set> extends LinkedHashMap<K, V> {
	public void add(K key, Object item) {
		if (!containsKey(key)) {
			put(key, (V) new LinkedHashSet());
		}
		get(key).add(item);
	}

	public void remove(K key, Object item) {
		if (containsKey(key)) {
			get(key).remove(item);
		}
	}
}
