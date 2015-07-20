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
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.PublicCloneable;

/**
 * chains of lookups - depth does not include the looked-up object: e.g.
 * class/id/instance would be depth 2
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
public class UnsortedMultikeyMap<V> extends MultikeyMapBase<V> implements
		PublicCloneable<UnsortedMultikeyMap> {
	public static class UnsortedMapCreator extends DelegateMapCreator {
		private static final long serialVersionUID = 1L;

		@Override
		public Map createDelegateMap(int depthFromRoot) {
			return new LinkedHashMap<>();
		}
	}

	private static final long serialVersionUID = 1L;

	@Override
	public UnsortedMultikeyMap clone() {
		try {
			UnsortedMultikeyMap clone = new UnsortedMultikeyMap();
			clone.delegate = createDelegateMap();
			clone.delegate.putAll(delegate);
			return clone;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

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
	protected DelegateMapCreator ensureDelegateMapCreator() {
		if (this.delegateMapCreator == null) {
			this.delegateMapCreator = new UnsortedMapCreator();
		}
		return delegateMapCreator;
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

	@Override
	public MultikeyMap createMap(int childDepth) {
		return new UnsortedMultikeyMap<V>(childDepth, depthFromRoot + 1,
				delegateMapCreator);
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		ensureDelegateMapCreator();
	}
}
