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

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@MappedSuperclass
@ClientInstantiable
@Introspectable
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
public abstract class AuthenticationSessionAttribute
		extends Entity<AuthenticationSessionAttribute> {
	private String key;

	private String serializedValue;

	@Transient
	public abstract AuthenticationSession getAuthenticationSession();

	@Override
	@Transient
	public long getId() {
		return id;
	}

	public String getKey() {
		return this.key;
	}

	@Transient
	public String getSerializedValue() {
		return this.serializedValue;
	}

	public abstract void setAuthenticationSession(
			AuthenticationSession authenticationSession);

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setSerializedValue(String serializedValue) {
		this.serializedValue = serializedValue;
	}
}
