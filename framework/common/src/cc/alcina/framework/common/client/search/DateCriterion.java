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

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;

/**
 * 
 * @author Nick Reddel
 */
public class DateCriterion extends AbstractDateCriterion {
	public DateCriterion() {
	}

	public DateCriterion(String displayName, Direction direction) {
		super(displayName);
		setDirection(direction);
	}

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (getDate() == null) {
			return result;
		}
		result.eql = "t." + getTargetPropertyName()
				+ (getDirection() == Direction.ASCENDING ? ">=" : "<") + " ? ";
		// round up if it's to...assume we're talking whole days here
		Date d = new Date(getDate().getTime()
				+ (long) (getDirection() == Direction.ASCENDING ? 0
						: 86400 * 1000));
		result.parameters.add(d);
		return result;
	}

	@Override
	public String toString() {
		return getDate() == null ? null
				: (getDirection() == Direction.ASCENDING ? " from " : " to ")
						+ CommonUtils.formatDate(getDate(),
								DateStyle.AU_DATE_SLASH);
	}
}