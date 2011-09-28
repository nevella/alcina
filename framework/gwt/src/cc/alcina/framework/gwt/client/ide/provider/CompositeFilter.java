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

package cc.alcina.framework.gwt.client.ide.provider;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilter;

/**
 *
 * @author Nick Reddel
 */

 public class CompositeFilter<T> implements CollectionFilter<T>{
	private List<CollectionFilter<T>> filters = new ArrayList<CollectionFilter<T>>();
	public boolean allow(T o) {
		for (CollectionFilter<T> filter : filters) {
			if (!filter.allow(o)){
				return false;
			}
		}
		return true;
	}
	public CompositeFilter<T> add(CollectionFilter<T> filter){
		filters.add(filter);
		return this;
	}
	
}