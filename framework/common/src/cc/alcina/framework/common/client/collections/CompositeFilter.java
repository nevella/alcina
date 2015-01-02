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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nick Reddel
 */
public class CompositeFilter<T> implements CollectionFilter<T> {
	private List<CollectionFilter<T>> filters = new ArrayList<CollectionFilter<T>>();

	private boolean or;

	public CompositeFilter() {
		this(false);
	}

	public CompositeFilter(boolean or) {
		this.or = or;
	}

	public boolean allow(T o) {
		for (CollectionFilter<T> filter : filters) {
			boolean allow = filter.allow(o);
			if (or && allow) {
				return true;
			}
			if (!or && !allow) {
				return false;
			}
		}
		return !or;
	}

	public CompositeFilter<T> add(CollectionFilter<T> filter) {
		filters.add(filter);
		return this;
	}
}