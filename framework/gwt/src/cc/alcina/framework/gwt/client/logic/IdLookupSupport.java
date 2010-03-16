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

package cc.alcina.framework.gwt.client.logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.HasId;


/**
 *
 * @author Nick Reddel
 */

 public class IdLookupSupport<V extends HasId> {
	private Map<Long, V> lookup;

	private Collection<V> colln;

	private boolean alwaysRefresh;
	public IdLookupSupport() {
	}
	
	public IdLookupSupport(boolean alwaysRefresh) {
		super();
		this.alwaysRefresh = alwaysRefresh;
	}

	public V lookup(Collection<V> colln, Long id) {
		if (this.colln == null || alwaysRefresh) {
			this.colln = colln;
			createLookup();
		}
		return lookup.get(id);
	}

	private void createLookup() {
		lookup = new HashMap<Long, V>();
		for (V v : colln) {
			lookup.put(v.getId(), v);
		}
	}
}
