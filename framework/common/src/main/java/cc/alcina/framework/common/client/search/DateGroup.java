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
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.DateUtil;

@TypeSerialization(flatSerializable = false)
public class DateGroup extends CriteriaGroup<AbstractDateCriterion> {
	public DateGroup() {
		super();
		setCombinator(FilterCombinator.AND);
	}

	public DateGroup(Date fromDate, Date toDate) {
		this();
		DateUtil.roundDate(fromDate, false);
		DateUtil.roundDate(toDate, true);
		AbstractDateCriterion dc = new DateCriterion("From",
				Direction.ASCENDING);
		dc.setValue(fromDate);
		addCriterion(dc);
		dc = new DateCriterion("To", Direction.DESCENDING);
		dc.setValue(toDate);
		addCriterion(dc);
	}

	@Override
	public Class entityClass() {
		return null;
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		return "Date";
	}

	@Override
	public String validatePermissions() {
		try {
			for (AbstractDateCriterion adc : getCriteria()) {
				adc.toString();
			}
		} catch (Exception e) {
			return "Access not permitted: (not date criterion)";
		}
		return null;
	}
}
