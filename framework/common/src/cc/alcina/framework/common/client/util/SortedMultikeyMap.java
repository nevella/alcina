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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * chains of lookups - depth does not include the looked-up object: e.g.
 * class/id/instance would be depth 2
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
public class SortedMultikeyMap<V> extends TreeMap implements MultikeyMap<V> {
	private final int depth;

	private MultikeyMapSupport multikeyMapSupport;

	public SortedMultikeyMap(int depth) {
		this.depth = depth;
		this.multikeyMapSupport = new MultikeyMapSupport(this);
	}

	@Override
	public void addValues(List<V> values) {
		multikeyMapSupport.addValues(values);
	}

	@Override
	public List<V> allValues() {
		return multikeyMapSupport.allValues();
	}

	@Override
	public TreeMap asMap(Object... objects) {
		return (TreeMap) multikeyMapSupport.asMap(true, objects);
	}

	public TreeMap asMapEnsure(boolean ensure, Object... objects) {
		return (TreeMap) multikeyMapSupport.asMap(ensure, objects);
	}

	@Override
	public MultikeyMap<V> createMap(int childDepth) {
		return new SortedMultikeyMap(childDepth);
	}

	@Override
	public V get(Object... objects) {
		return (V) multikeyMapSupport.getEnsure(true, objects);
	}

	@Override
	public int getDepth() {
		return this.depth;
	}

	@Override
	public V getEnsure(boolean ensure, Object... objects) {
		return (V) multikeyMapSupport.getEnsure(ensure, objects);
	}

	@Override
	public <T> Collection<T> items(Object... objects) {
		if (objects.length >= depth) {
			throw new IllegalArgumentException(
					"items() must have fewer than <depth> keys");
		}
		return keys(objects);
	}

	@Override
	public <T> Collection<T> keys(Object... objects) {
		Map m = asMapEnsure(false, objects);
		return m == null ? null : m.keySet();
	}

	@Override
	public void put(Object... objects) {
		multikeyMapSupport.put(objects);
	}

	@Override
	public V remove(Object... objects) {
		return (V) multikeyMapSupport.remove(objects);
	}

	@Override
	public <T> Collection<T> reverseItems(Object... objects) {
		if (objects.length >= depth) {
			throw new IllegalArgumentException(
					"items() must have fewer than <depth> keys");
		}
		return reverseKeys(objects);
	}

	@Override
	public <T> Collection<T> reverseKeys(Object... objects) {
		TreeMap m = asMapEnsure(false, objects);
		return m == null ? null : m.descendingMap().keySet();
	}

	@Override
	public <T> Collection<T> reverseValues(Object... objects) {
		TreeMap m = asMapEnsure(false, objects);
		return m == null ? null : m.descendingMap().values();
	}

	@Override
	public MultikeyMap<V> swapKeysZeroAndOne() {
		return multikeyMapSupport.swapKeysZeroAndOne();
	}

	@Override
	public <T> Collection<T> values(Object... objects) {
		Map m = asMapEnsure(false, objects);
		return m == null ? null : m.values();
	}

	@Override
	public boolean containsKey(Object... objects) {
		return multikeyMapSupport.containsKey(objects);
	}
}
