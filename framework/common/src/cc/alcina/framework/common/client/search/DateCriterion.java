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
	static final transient long serialVersionUID = -1L;

	public DateCriterion() {
		setOperator(StandardSearchOperator.EQUALS);
	}

	public DateCriterion(String displayName, Direction direction) {
		super(displayName);
		setDirection(direction);
		setOperator(StandardSearchOperator.EQUALS);
	}

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (getDate() == null) {
			return result;
		}
		result.eql = targetPropertyNameWithTable()
				+ (getDirection() == Direction.ASCENDING ? ">=" : "<") + " ? ";
		// round up if it's to...assume we're talking whole days here
		Date d = new Date(getDate().getTime()
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
		if (getDate() == null) {
			return null;
		}
		String displayName = getDisplayName() != null ? getDisplayName()
				: getDirection() == Direction.ASCENDING ? "from" : "to";
		return Ax.format("%s %s", displayName,
				CommonUtils.formatDate(getDate(), DateStyle.AU_DATE_SLASH));
	}
}
