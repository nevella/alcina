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

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public class ObjectRef implements Serializable {
	private long id;

	private long localId;

	private ClassRef classRef;

	private int versionNumber;

	public ObjectRef() {
	}

	public ObjectRef(Entity entity) {
		setClassRef(ClassRef.forClass(entity.entityClass()));
		setId(entity.getId());
		setLocalId(entity.getLocalId());
		if (entity instanceof HasVersionNumber) {
			setVersionNumber(((HasVersionNumber) entity).getVersionNumber());
		}
	}

	public ClassRef getClassRef() {
		return this.classRef;
	}

	public long getId() {
		return this.id;
	}

	public long getLocalId() {
		return this.localId;
	}

	public int getVersionNumber() {
		return this.versionNumber;
	}

	public void setClassRef(ClassRef classRef) {
		this.classRef = classRef;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setLocalId(long localId) {
		this.localId = localId;
	}

	public void setVersionNumber(int versionNumber) {
		this.versionNumber = versionNumber;
	}

	@Override
	public String toString() {
		return Ax.format("%s:%s,%s,%s",
				CommonUtils.simpleClassName(classRef.getRefClass()), id,
				localId, versionNumber);
	}
}
