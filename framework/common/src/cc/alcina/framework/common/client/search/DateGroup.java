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

import java.util.Date;

import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.util.CommonUtils;


@BeanInfo(displayNamePropertyName = "displayName")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class DateGroup extends CriteriaGroup {
	public DateGroup() {
		super();
		setCombinator(FilterCombinator.AND);
		setDisplayName("Date");
	}


	public DateGroup(String propertyName, Date fromDate, Date toDate) {
		this();
		CommonUtils.roundDate(fromDate, false);
		CommonUtils.roundDate(toDate, true);
		DateCriterion dc = new DateCriterion("From", propertyName,
				Direction.ASCENDING);
		dc.setDate(fromDate);
		addCriterion(dc);
		dc = new DateCriterion("To", propertyName,
				Direction.DESCENDING);
		dc.setDate(toDate);
		addCriterion(dc);
	}
}