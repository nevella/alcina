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

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;


/**
 *
 * @author Nick Reddel
 */

 public class ObjectRef implements Serializable {
	private long id;

	private long localId;

	private ClassRef classRef;

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

	public ClassRef getClassRef() {
		return this.classRef;
	}

	public void setClassRef(ClassRef classRef) {
		this.classRef = classRef;
	}

	public ObjectRef() {
	}

	public ObjectRef(HasIdAndLocalId hili) {
		setClassRef(ClassRef.forClass(hili.getClass()));
		setId(hili.getId());
		setLocalId(hili.getLocalId());
	}
}
