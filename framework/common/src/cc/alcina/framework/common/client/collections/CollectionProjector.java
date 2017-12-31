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
package cc.alcina.framework.common.client.collections;

import com.totsp.gwittir.client.beans.Converter;

/**
 * 
 * @author Nick Reddel
 */
public abstract class CollectionProjector<T, V> implements Converter<T, V> {
	private V bestValue = null;

	private T bestSource = null;

	public T getBestSource() {
		return this.bestSource;
	}

	public V getBestValue() {
		return this.bestValue;
	}

	public void tryProject(T t) {
		V value = convert(t);
		if (isMoreDesirable(value, bestValue)) {
			bestValue = value;
			bestSource = t;
		}
	}

	protected abstract boolean isMoreDesirable(V value, V bestValue);
}
