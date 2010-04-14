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
package cc.alcina.framework.common.client.entity;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasOwner;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.Permissible;

/**
 * 
 * @author Nick Reddel
 */
public class GwtPersistableObject extends BaseBindable implements
		HasIdAndLocalId, Permissible, HasOwner {
	private long id;

	private long localId;

	private transient IUser owner;

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getLocalId() {
		return this.localId;
	}

	public void setLocalId(long localId) {
		this.localId = localId;
	}

	/**
	 * Hack - note that the old/newvalues of the propertychangeevent should
	 * !not! be read. For listeners on collection properties
	 */
	public void fireNullPropertyChange(String name) {
		((MutablePropertyChangeSupport)this.propertyChangeSupport).fireNullPropertyChange(name);
	}

	public AccessLevel accessLevel() {
		return AccessLevel.ADMIN;
	}

	public String rule() {
		return null;
	}

	public void setOwner(IUser owner) {
		this.owner = owner;
	}

	/**
	 * This will only be used for permissions checking server-side, no need to
	 * send to the client
	 */
	@XmlTransient
	public IUser getOwner() {
		return owner;
	}
}
