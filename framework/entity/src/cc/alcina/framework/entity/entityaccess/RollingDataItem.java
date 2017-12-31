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
package cc.alcina.framework.entity.entityaccess;

import java.util.Date;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;

@MappedSuperclass
@DomainTransformPersistable
public abstract class RollingDataItem extends AbstractDomainBase {
	private String data;

	private String maxKey;

	private Date date;

	private String typeKey;

	@Lob
	@Transient
	public String getData() {
		return this.data;
	}

	public Date getDate() {
		return this.date;
	}

	public String getMaxKey() {
		return this.maxKey;
	}

	public String getTypeKey() {
		return this.typeKey;
	}

	public void setData(String data) {
		String old_data = this.data;
		this.data = data;
		propertyChangeSupport().firePropertyChange("data", old_data, data);
	}

	public void setDate(Date date) {
		Date old_date = this.date;
		this.date = date;
		propertyChangeSupport().firePropertyChange("date", old_date, date);
	}

	public void setMaxKey(String maxKey) {
		String old_maxKey = this.maxKey;
		this.maxKey = maxKey;
		propertyChangeSupport().firePropertyChange("maxKey", old_maxKey,
				maxKey);
	}

	public void setTypeKey(String typeKey) {
		String old_typeKey = this.typeKey;
		this.typeKey = typeKey;
		propertyChangeSupport().firePropertyChange("typeKey", old_typeKey,
				typeKey);
	}
}
