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
package cc.alcina.framework.common.client.search;

import java.util.Comparator;

/**
 * 
 * @author Nick Reddel
 */
public class OrderCriterion extends SearchCriterion {
	static final transient long serialVersionUID = -1L;

	public String addDirection(String criterionName) {
		return criterionName == null || getDirection() == Direction.ASCENDING
				? criterionName
				: criterionName + " (reverse)";
	}

	@Override
	public OrderCriterion withDirection(Direction direction) {
		return (OrderCriterion) super.withDirection(direction);
	}

	public Comparator reverseIfDescending(Comparator cmp) {
		return getDirection() == Direction.DESCENDING ? cmp.reversed()
				: cmp;
	}
}
