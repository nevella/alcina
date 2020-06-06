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

import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasOwner;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.util.Ax;

/**
 * 
 * @author Nick Reddel
 */
public class WrapperPersistable extends Entity<WrapperPersistable>
		implements Permissible, HasOwner {
	private long id;

	private transient IUser owner;

	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.ADMIN;
	}

	/**
	 * Hack - note that the old/newvalues of the propertychangeevent should
	 * !not! be read. For listeners on collection properties
	 */
	@Override
	public void fireNullPropertyChange(String name) {
		((MutablePropertyChangeSupport) this.propertyChangeSupport())
				.fireNullPropertyChange(name);
	}

	@Override
	public long getId() {
		return this.id;
	}

	/**
	 * This will only be used for permissions checking server-side, no need to
	 * send to the client
	 */
	@Override
	@XmlTransient
	public IUser getOwner() {
		return owner;
	}

	@Override
	public String rule() {
		return null;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setOwner(IUser owner) {
		this.owner = owner;
	}

	@Override
	public String toString() {
		return Ax.format("Wrapper persistable: %s/%s",
				getClass().getSimpleName(), getId());
	}
}
