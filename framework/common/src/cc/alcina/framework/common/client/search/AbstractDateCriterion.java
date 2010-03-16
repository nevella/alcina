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
 * @author Nick Reddel
 */

 public class AbstractDateCriterion extends SearchCriterion {
	private Date date;

	public AbstractDateCriterion() {
		super();
	}

	public AbstractDateCriterion(Date date) {
		setDate(date);
	}
	public AbstractDateCriterion(String displayName,Date date) {
		this(displayName);
		setDate(date);
	}

	public AbstractDateCriterion(String displayName) {
		super(displayName);
	}

	public AbstractDateCriterion(String displayName, String propertyName) {
		super(displayName, propertyName);
	}

	public void setDate(Date date) {
		Date old_date = this.date;
		this.date = date;
		propertyChangeSupport.firePropertyChange("date", old_date, date);
	}

	public Date getDate() {
		return date;
	}
}