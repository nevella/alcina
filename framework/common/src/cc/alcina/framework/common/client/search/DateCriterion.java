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

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/**
 *
 * @author Nick Reddel
 */
public class DateCriterion extends AbstractDateCriterion {
	public DateCriterion() {
		setOperator(StandardSearchOperator.EQUALS);
	}

	public DateCriterion(String displayName, Direction direction) {
		super(displayName);
		setDirection(direction);
		setOperator(StandardSearchOperator.EQUALS);
	}

	@Override
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (getValue() == null) {
			return result;
		}
		result.eql = targetPropertyNameWithTable()
				+ (getDirection() == Direction.ASCENDING ? ">=" : "<") + " ? ";
		// round up if it's to...assume we're talking whole days here
		Date d = new Date(getValue().getTime()
				+ (long) (getDirection() == Direction.ASCENDING ? 0
						: 86400 * 1000));
		result.parameters.add(d);
		return result;
	}

	public boolean rangeControlledByDirection() {
		return false;
	}

	@Override
	public String toString() {
		return toStringWithDisplayName(true);
	}

	public String toStringWithDisplayName(boolean withDisplayName) {
		if (getValue() == null) {
			return null;
		}
		String displayName = getDisplayName() != null ? getDisplayName()
				: getDirection() == Direction.ASCENDING ? "from" : "to";
		return withDisplayName
				? Ax.format("%s %s", displayName,
						CommonUtils.formatDate(getValue(),
								DateStyle.AU_DATE_SLASH))
				: CommonUtils.formatDate(getValue(), DateStyle.AU_DATE_SLASH);
	}
}
