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

package cc.alcina.framework.entity.util;

import java.util.Comparator;
import java.util.List;

import cc.alcina.framework.common.client.logic.permissions.HasId;


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class IdOrderingComparator implements Comparator<HasId> {
	private final List<Long> idsToOrderBy;

	public IdOrderingComparator(List<Long> idsToOrderBy) {
		this.idsToOrderBy = idsToOrderBy;
	}

	public int compare(HasId o1, HasId o2) {
		int thisVal = idsToOrderBy.indexOf(o1.getId());
		int anotherVal = idsToOrderBy.indexOf(o2.getId());
		return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
	}
}
