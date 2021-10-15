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
import java.util.function.Predicate;

import cc.alcina.framework.common.client.util.CommonUtils;

/**
 *
 * @author Nick Reddel
 */
public class CompositeFilter<T> implements Predicate<T> {
	private List<Predicate<T>> predicates = new ArrayList<Predicate<T>>();

	private boolean or;

	public CompositeFilter() {
		this(false);
	}

	public CompositeFilter(boolean or) {
		this.or = or;
	}

	public CompositeFilter<T> add(Predicate<T> filter) {
		predicates.add(filter);
		return this;
	}

	public List<Predicate<T>> getFilters() {
		return this.predicates;
	}

	@Override
	public boolean test(T o) {
		for (Predicate<T> filter : predicates) {
			boolean allow = filter.test(o);
			if (or && allow) {
				return true;
			}
			if (!or && !allow) {
				return false;
			}
		}
		return !or;
	}

	@Override
	public String toString() {
		return "(" + CommonUtils.join(predicates, (or ? " OR " : " AND "))
				+ ")";
	}
}