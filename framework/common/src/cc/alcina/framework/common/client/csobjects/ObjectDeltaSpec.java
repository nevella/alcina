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

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ObjectRef;


/**
 *
 * @author Nick Reddel
 */

 public class ObjectDeltaSpec implements Serializable {
	private ObjectRef objectRef;

	private String propertyName;

	public ObjectDeltaSpec() {
	}

	public ObjectDeltaSpec(HasIdAndLocalId hili, String propertyName) {
		setObjectRef(new ObjectRef(hili));
		setPropertyName(propertyName);
	}

	public ObjectRef getObjectRef() {
		return this.objectRef;
	}

	public void setObjectRef(ObjectRef objectRef) {
		this.objectRef = objectRef;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
}
