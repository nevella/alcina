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

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 *
 * @author Nick Reddel
 */
public class AbstractDateCriterion extends SearchCriterion
		implements HasValue<Date> {
	private Date value;

	private Direction direction = Direction.ASCENDING;

	public AbstractDateCriterion() {
		super();
	}

	public AbstractDateCriterion(Date date) {
		setValue(date);
	}

	public AbstractDateCriterion(String displayName) {
		super(displayName);
	}

	public AbstractDateCriterion(String displayName, Date date) {
		this(displayName);
		setValue(date);
	}

	@PropertySerialization(path = "dir")
	public Direction getDirection() {
		return this.direction;
	}

	@SuppressWarnings("deprecation")
	@Override
	@PropertySerialization(defaultProperty = true)
	public Date getValue() {
		if (value != null) {
			try {
				int year = value.getYear();
				if (year < -10000) {
					value = new Date(value.getTime());
					value.setYear(-10000);
				} else if (year > 10000) {
					value = new Date(value.getTime());
					value.setYear(10000);
				}
			} catch (NullPointerException e) {
				// parallel call issues?
			}
		}
		return value;
	}

	public void setDirection(Direction direction) {
		Direction old_direction = this.direction;
		this.direction = direction;
		propertyChangeSupport().firePropertyChange("direction", old_direction,
				direction);
	}

	@Override
	public void setValue(Date value) {
		Date old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	public AbstractDateCriterion withDate(int year, int month, int dayOfMonth) {
		setValue(CommonUtils.oldDate(year, month, dayOfMonth));
		return this;
	}

	public AbstractDateCriterion withDirection(Direction direction) {
		setDirection(direction);
		return this;
	}

	public AbstractDateCriterion withValue(Date date) {
		setValue(date);
		return this;
	}
}
