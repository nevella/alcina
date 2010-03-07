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


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class DateCriterion extends SearchCriterion {
	public DateCriterion() {
	}
	public DateCriterion(String displayName, String propertyName,
			Direction direction) {
		super(displayName, propertyName);
		setDirection(direction);
	}

	private Date date;

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (date==null){
			return result;
		}
		result.eql = "t."+getTargetPropertyName()
				+ (getDirection() == Direction.ASCENDING ? ">=" : "<") + " ? ";
		result.parameters.add(date);
		return result;
	}

	public void setDate(Date date) {
		Date old_date = this.date;
		this.date = date;
		propertyChangeSupport.firePropertyChange("date", old_date, date);
	}

	public Date getDate() {
		return date;
	}

	@Override
	public String renderablePropertyName() {
		return "date";
	}
}