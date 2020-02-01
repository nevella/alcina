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

import java.util.Set;

import cc.alcina.framework.common.client.domain.DomainCollections;

/**
 * 
 * @author nick@alcina.cc
 * 
 * @param <K>
 * @param <V>
 */
@SuppressWarnings("unchecked")
public class SortedMultiset<K, V extends Set> extends Multiset<K, V> {
	@Override
	protected Set createSet() {
		return DomainCollections.get().createSortedSet();
	}
}
