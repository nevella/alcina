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

/**
 * chains of lookups - depth does not include the looked-up object: e.g.
 * class/id/instance would be depth 2
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
public class SortedMultikeyMap<V> implements MultikeyMap<V>, Serializable {
	private static final long serialVersionUID = 1L;

	private int depth;

	private MultikeyMapSupport multikeyMapSupport;

	private TreeMap delegate;

	private transient ImmutableMap readonlyDelegate;

	public SortedMultikeyMap() {
		this(2);
	}

	public SortedMultikeyMap(int depth) {
		this.depth = depth;
		this.delegate = new TreeMap();
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
	public SortedMultikeyMap asMap(Object... objects) {
		return (SortedMultikeyMap) multikeyMapSupport.asMap(true, objects);
	}

	public SortedMultikeyMap asMapEnsure(boolean ensure, Object... objects) {
		return (SortedMultikeyMap) multikeyMapSupport.asMap(ensure, objects);
	}

	private TreeMap asMapEnsureDelegate(boolean ensure, Object... objects) {
		SortedMultikeyMap mkm = (SortedMultikeyMap) multikeyMapSupport.asMap(
				ensure, objects);
		return mkm == null ? null : mkm.delegate;
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public boolean containsKey(Object... objects) {
		return multikeyMapSupport.containsKey(objects);
	}

	@Override
	public MultikeyMap<V> createMap(int childDepth) {
		return new SortedMultikeyMap(childDepth);
	}

	@Override
	public Map delegate() {
		if (readonlyDelegate == null) {
			readonlyDelegate = new ImmutableMap(delegate);
		}
		return readonlyDelegate;
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

	public boolean isEmpty() {
		return this.delegate.isEmpty();
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
		Map m = asMapEnsureDelegate(false, objects);
		return m == null ? null : m.keySet();
	}

	public Set keySet() {
		return this.delegate.keySet();
	}

	@Override
	public void put(Object... objects) {
		multikeyMapSupport.put(objects);
	}

	@Override
	public void putMulti(MultikeyMap<V> multi) {
		multikeyMapSupport.putMulti(multi);
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
		TreeMap m = asMapEnsureDelegate(false, objects);
		return m == null ? null : m.descendingMap().keySet();
	}

	@Override
	public <T> Collection<T> reverseValues(Object... objects) {
		TreeMap m = asMapEnsureDelegate(false, objects);
		return m == null ? null : m.descendingMap().values();
	}

	@Override
	public void setDepth(int depth) {
		if (!delegate.keySet().isEmpty()) {
			throw new RuntimeException("cannot change depth once items added");
		}
		this.depth = depth;
	}

	public int size() {
		return this.delegate.size();
	}

	@Override
	public MultikeyMap<V> swapKeysZeroAndOne() {
		return multikeyMapSupport.swapKeysZeroAndOne();
	}

	@Override
	public <T> Collection<T> values(Object... objects) {
		Map m = asMapEnsureDelegate(false, objects);
		return m == null ? null : m.values();
	}

	@Override
	public Map writeableDelegate() {
		return delegate;
	}

	public List<List> asTuples() {
		return multikeyMapSupport.asTuples();
	}

	@Override
	public boolean checkKeys(Object[] keys) {
		for (Object object : keys) {
			if(object==null){
				return false;
			}
		}
		return true;
	}
}
