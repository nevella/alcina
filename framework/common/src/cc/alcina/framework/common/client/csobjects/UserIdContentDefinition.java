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

package cc.alcina.framework.common.client.csobjects;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.publication.ContentDefinition;


/**
 *
 * @author Nick Reddel
 */

 public abstract class UserIdContentDefinition extends GwtPersistableObject implements
		ContentDefinition, GwtMultiplePersistable {
	private long userId;

	private transient IUser user;

	public long getUserId() {
		return this.userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	@XmlTransient
	public IUser getUser() {
		return this.user;
	}

	public void setUser(IUser user) {
		this.user = user;
	}

	@Override
	public String toString() {
		String s = getUser() == null ? "" : getUser().getUserName();
		return s + "(" + getUserId() + ")";
	}

	
}
