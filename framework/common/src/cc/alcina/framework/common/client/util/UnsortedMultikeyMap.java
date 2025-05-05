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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.core.shared.GwtIncompatible;

import cc.alcina.framework.common.client.collections.PublicCloneable;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;

/**
 * chains of lookups - depth does not include the looked-up object: e.g.
 * class/id/instance would be depth 2
 *
 * 
 *
 */
public class UnsortedMultikeyMap<V> extends MultikeyMapBase<V>
		implements PublicCloneable<UnsortedMultikeyMap> {
	private static final long serialVersionUID = 1L;

	/**
	 * Ensures that RPC will consider type parameter V to be exposed. It will be
	 * pruned by dead code elimination.
	 */
	@SuppressWarnings("unused")
	private V exposeValue;

	public UnsortedMultikeyMap() {
		this(2);
	}

	public UnsortedMultikeyMap(int depth) {
		this(depth, 0);
	}

	public UnsortedMultikeyMap(int depth, int depthFromRoot) {
		super(depth, depthFromRoot);
	}

	public UnsortedMultikeyMap(int depth, int depthFromRoot,
			DelegateMapCreator delegateMapCreator) {
		super(depth, depthFromRoot, delegateMapCreator);
	}

	@Override
	public UnsortedMultikeyMap clone() {
		UnsortedMultikeyMap clone = new UnsortedMultikeyMap();
		asTuples(depth).forEach(tuple -> {
			Object[] array = (Object[]) tuple.toArray(new Object[tuple.size()]);
			clone.put(array);
		});
		return clone;
	}

	@Override
	public MultikeyMap createMap(int childDepth) {
		return new UnsortedMultikeyMap<V>(childDepth,
				depthFromRoot + (depth - childDepth), delegateMapCreator);
	}

	@Override
	protected DelegateMapCreator ensureDelegateMapCreator() {
		if (this.delegateMapCreator == null) {
			this.delegateMapCreator = CollectionCreators.Bootstrap
					.getUnsortedMapCreator();
		}
		return delegateMapCreator;
	}

	@GwtIncompatible
	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		ensureDelegateMapCreator();
	}

	@Override
	public <T> Collection<T> reverseKeys(Object... objects) {
		throw new UnsupportedOperationException(
				"Use a sorted multikey map, or reverse yourself");
	}

	@Override
	public <T> Collection<T> reverseValues(Object... objects) {
		throw new UnsupportedOperationException(
				"Use a sorted multikey map, or reverse yourself");
	}

	/*
	 * For backwards (serialization) compatibility
	 */
	public static class UnsortedMapCreator implements DelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			return new LinkedHashMap<>();
		}
	}
}
