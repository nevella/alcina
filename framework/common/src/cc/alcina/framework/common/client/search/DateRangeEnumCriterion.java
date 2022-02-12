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

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

/**
 * @author Nick Reddel
 */
@TypeSerialization("daterange")
public class DateRangeEnumCriterion extends EnumCriterion<DateRange> {
	private DateRange dateRange;

	public DateRangeEnumCriterion() {
	}

	public DateRangeEnumCriterion(DateRange value) {
		setValue(value);
	}

	public DateRangeEnumCriterion(String criteriaDisplayName,
			boolean withNull) {
		super(criteriaDisplayName, withNull);
	}

	@AlcinaTransient
	public DateRange getDateRange() {
		return this.dateRange;
	}

	@Override
	@PropertySerialization(defaultProperty = true)
	public DateRange getValue() {
		return getDateRange();
	}

	public void setDateRange(DateRange dateRange) {
		DateRange old_dateRange = this.dateRange;
		this.dateRange = dateRange;
		propertyChangeSupport().firePropertyChange("dateRange", old_dateRange,
				dateRange);
	}

	@Override
	public void setValue(DateRange value) {
		setDateRange(value);
	}

	@Override
	protected boolean valueAsString() {
		return true;
	}
}
