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

import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.util.Ax;

@DomainTransformPersistable
@MappedSuperclass
public abstract class AuthenticationSession
		extends Entity<AuthenticationSession> implements HasIUser {
	private Date startTime;

	private Date endTime;

	private String sessionId;

	private int maxInstances;

	private String authenticationType;

	private Date lastAccessed;

	private String endReason;

	@Override
	public String toString() {
		return Ax.format(
				"%s :: {id: %s - user: %s - start: %s - type: %s - end reason: %s",
				toStringEntity(), sessionId, getUser(), startTime,
				authenticationType, endReason);
	}

	@Transient
	public abstract Set<? extends AuthenticationSessionAttribute>
			getAttributes();

	public String getAuthenticationType() {
		return this.authenticationType;
	}

	@Transient
	public abstract Set<? extends ClientInstance> getClientInstances();

	public String getEndReason() {
		return this.endReason;
	}

	public Date getEndTime() {
		return this.endTime;
	}

	@Override
	@Transient
	public long getId() {
		return id;
	}

	@Transient
	public abstract Iid getIid();

	public Date getLastAccessed() {
		return this.lastAccessed;
	}

	public int getMaxInstances() {
		return this.maxInstances;
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public Date getStartTime() {
		return this.startTime;
	}

	public boolean provideIsExpired() {
		return endTime != null;
	}

	public Optional<Date> provideLastAccessed() {
		return getClientInstances().stream()
				.map(ClientInstance::getLastAccessed)
				.max(Comparator.naturalOrder());
	}

	public void setAuthenticationType(String authenticationType) {
		String old_authenticationType = this.authenticationType;
		this.authenticationType = authenticationType;
		propertyChangeSupport().firePropertyChange("authenticationType",
				old_authenticationType, authenticationType);
	}

	public void setEndReason(String endReason) {
		String old_endReason = this.endReason;
		this.endReason = endReason;
		propertyChangeSupport().firePropertyChange("endReason", old_endReason,
				endReason);
	}

	public void setEndTime(Date endTime) {
		Date old_endTime = this.endTime;
		this.endTime = endTime;
		propertyChangeSupport().firePropertyChange("endTime", old_endTime,
				endTime);
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public abstract void setIid(Iid iid);

	public void setLastAccessed(Date lastAccessed) {
		Date old_lastAccessed = this.lastAccessed;
		this.lastAccessed = lastAccessed;
		propertyChangeSupport().firePropertyChange("lastAccessed",
				old_lastAccessed, lastAccessed);
	}

	public void setMaxInstances(int maxInstances) {
		int old_maxInstances = this.maxInstances;
		this.maxInstances = maxInstances;
		propertyChangeSupport().firePropertyChange("maxInstances",
				old_maxInstances, maxInstances);
	}

	public void setSessionId(String sessionId) {
		String old_sessionId = this.sessionId;
		this.sessionId = sessionId;
		propertyChangeSupport().firePropertyChange("sessionId", old_sessionId,
				sessionId);
	}

	public void setStartTime(Date startTime) {
		Date old_startTime = this.startTime;
		this.startTime = startTime;
		propertyChangeSupport().firePropertyChange("startTime", old_startTime,
				startTime);
	}
}
