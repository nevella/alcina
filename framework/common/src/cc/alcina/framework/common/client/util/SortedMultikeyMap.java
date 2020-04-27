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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.gwt.core.shared.GwtIncompatible;

import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;

/**
 * chains of lookups - depth does not include the looked-up object: e.g.
 * class/id/instance would be depth 2
 * 
 * @author nick@alcina.cc
 * 
 */
public class SortedMultikeyMap<V> extends MultikeyMapBase<V> {
	static final transient long serialVersionUID = -1L;

	/**
	 * Ensures that RPC will consider type parameter V to be exposed. It will be
	 * pruned by dead code elimination.
	 */
	@SuppressWarnings("unused")
	private V exposeValue;

	public SortedMultikeyMap() {
		this(2);
	}

	public SortedMultikeyMap(int depth) {
		this(depth, 0);
	}

	public SortedMultikeyMap(int depth, int depthFromRoot) {
		super(depth, depthFromRoot);
	}

	public SortedMultikeyMap(int depth, int depthFromRoot,
			DelegateMapCreator delegateMapCreator) {
		super(depth, depthFromRoot, delegateMapCreator);
	}

	@Override
	public boolean checkKeys(Object[] keys) {
		for (Object object : keys) {
			if (object == null) {
				return false;
			}
		}
		return true;
	}

	@Override
	public MultikeyMap<V> createMap(int childDepth) {
		return new SortedMultikeyMap(childDepth, depthFromRoot + 1,
				delegateMapCreator);
	}

	@Override
	public <T> Collection<T> reverseKeys(Object... objects) {
		NavigableMap nm = (NavigableMap) asMapEnsureDelegate(false, objects);
		return nm == null ? new ArrayList<>() : nm.descendingKeySet();
	}

	@Override
	public <T> Collection<T> reverseValues(Object... objects) {
		throw new RuntimeException("Values are not sorted");
		// SortedMap m = (SortedMap) asMapEnsureDelegate(false, objects);
		// NavigableMap nm=navigableMap(m);
		// return nm == null ? null : nm.descendingMap().keySet();
	}

	@GwtIncompatible
	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		ensureDelegateMapCreator();
	}

	@Override
	protected DelegateMapCreator ensureDelegateMapCreator() {
		if (this.delegateMapCreator == null) {
			this.delegateMapCreator = new SortedMapCreator();
		}
		return this.delegateMapCreator;
	}

	public static class SortedMapCreator implements DelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new TreeMap(new NullFriendlyComparatorWrapper<>());
		}
	}
}
