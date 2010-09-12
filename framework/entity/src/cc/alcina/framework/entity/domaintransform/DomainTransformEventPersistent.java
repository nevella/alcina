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
package cc.alcina.framework.entity.domaintransform;

import java.util.Date;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.entity.ResourceUtilities;

@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */
public abstract class DomainTransformEventPersistent extends
		DomainTransformEvent implements HasId {
	private long id;

	private DomainTransformRequestPersistent domainTransformRequestPersistent;

	private Date serverCommitDate;

	// persistence in app subclass
	@Transient
	public DomainTransformRequestPersistent getDomainTransformRequestPersistent() {
		return domainTransformRequestPersistent;
	}

	@Transient
	public long getId() {
		return this.id;
	}

	public Date getServerCommitDate() {
		return serverCommitDate;
	}

	@Transient
	public abstract IUser getUser();

	public void setDomainTransformRequestPersistent(
			DomainTransformRequestPersistent domainTransformRequestPersistent) {
		this.domainTransformRequestPersistent = domainTransformRequestPersistent;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setServerCommitDate(Date serverCommitDate) {
		this.serverCommitDate = serverCommitDate;
	}

	public abstract void setUser(IUser user);

	public abstract void wrap(DomainTransformEvent evt);

	public DomainTransformEvent toSimpleEvent() {
		DomainTransformEvent event = new DomainTransformEvent();
		ResourceUtilities.copyBeanProperties(this, event, null, true);
		event.setEventId(getId());
		return event;
	}
}
