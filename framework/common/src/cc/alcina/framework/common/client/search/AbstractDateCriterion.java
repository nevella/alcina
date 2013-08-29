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

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public class AbstractDateCriterion extends SearchCriterion implements
		HasValue<Date> {
	static final transient long serialVersionUID = -1L;

	private Date date;

	public AbstractDateCriterion() {
		super();
	}

	public AbstractDateCriterion(Date date) {
		setDate(date);
	}

	@Override
	protected <SC extends SearchCriterion> SC copyPropertiesFrom(SC copyFromCriterion) {
		Date copyFromDate = ((AbstractDateCriterion) copyFromCriterion).date;
		date = copyFromDate == null ? null : new Date(copyFromDate.getTime());
		return super.copyPropertiesFrom(copyFromCriterion);
	}

	public AbstractDateCriterion(String displayName, Date date) {
		this(displayName);
		setDate(date);
	}

	public AbstractDateCriterion(String displayName) {
		super(displayName);
	}

	public boolean equivalentTo(SearchCriterion other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		AbstractDateCriterion otherImpl = (AbstractDateCriterion) other;
		return otherImpl.getDirection() == getDirection()
				&& CommonUtils.equalsWithNullEquality(getDate(),
						otherImpl.getDate());
	}

	public void setDate(Date date) {
		Date old_date = this.date;
		this.date = date;
		propertyChangeSupport().firePropertyChange("date", old_date, date);
	}

	@SuppressWarnings("deprecation")
	public Date getDate() {
		if(date!=null){
			int year = date.getYear();
			if(year<-10000){
				date=new Date(date.getTime());
				date.setYear(-10000);
			}else if (year>10000){
				date=new Date(date.getTime());
				date.setYear(10000);
			}
		}
		return date;
	}

	@XmlTransient
	public Date getValue() {
		return getDate();
	}

	/**
	 * add property change firing to the subclass implementation, if you care
	 */
	public void setValue(Date value) {
		setDate(value);
	}
}