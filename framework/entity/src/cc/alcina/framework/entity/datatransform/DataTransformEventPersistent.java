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

package cc.alcina.framework.entity.datatransform;

import java.util.Date;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformEvent;
import cc.alcina.framework.common.client.logic.permissions.IUser;


@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */

 public abstract class DataTransformEventPersistent extends DataTransformEvent
		implements HasId {
	private long id;

	private DataTransformRequestPersistent dataTransformRequestPersistent;

	private Date serverCommitDate;

	// persistence in app subclass
	@Transient
	public DataTransformRequestPersistent getDataTransformRequestPersistent() {
		return dataTransformRequestPersistent;
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

	public void setDataTransformRequestPersistent(
			DataTransformRequestPersistent dataTransformRequestPersistent) {
		this.dataTransformRequestPersistent = dataTransformRequestPersistent;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setServerCommitDate(Date serverCommitDate) {
		this.serverCommitDate = serverCommitDate;
	}

	public abstract void setUser(IUser user);

	public abstract void wrap(DataTransformEvent evt);
}
