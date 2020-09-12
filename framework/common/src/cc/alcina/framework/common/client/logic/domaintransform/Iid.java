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
package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.Set;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;

/**
 *
 * @author Nick Reddel
 */
@DomainTransformPersistable
@MappedSuperclass
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
public abstract class Iid extends VersionableEntity<Iid> {
	private String instanceId;

	private Long rememberMeUser_id;

	@Transient
	public abstract Set<? extends AuthenticationSession>
			getAuthenticationSessions();

	@Override
	@Transient
	public long getId() {
		return id;
	}

	public String getInstanceId() {
		return this.instanceId;
	}

	// FIXME - mvcc.5 - remove
	public Long getRememberMeUser_id() {
		return this.rememberMeUser_id;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setInstanceId(String instanceId) {
		String old_instanceId = this.instanceId;
		this.instanceId = instanceId;
		propertyChangeSupport().firePropertyChange("instanceId", old_instanceId,
				instanceId);
	}

	public void setRememberMeUser_id(Long rememberMeUser_id) {
		Long old_rememberMeUser_id = this.rememberMeUser_id;
		this.rememberMeUser_id = rememberMeUser_id;
		propertyChangeSupport().firePropertyChange("rememberMeUser_id",
				old_rememberMeUser_id, rememberMeUser_id);
	}
}