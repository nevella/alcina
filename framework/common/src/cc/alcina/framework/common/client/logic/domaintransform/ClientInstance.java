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
import java.util.Date;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

@MappedSuperclass
@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
@Introspectable
public abstract class ClientInstance implements HasIUser, HasIdAndLocalId,
		Serializable, Cloneable {
	private long id;

	private long localId;

	private Date helloDate;

	private Integer auth;

	private String userAgent;

	public Integer getAuth() {
		return auth;
	}

	public Date getHelloDate() {
		return helloDate;
	}

	@Transient
	public long getId() {
		return id;
	}

	public abstract ClientInstance clone();

	public ClientInstance copyPropertiesTo(ClientInstance other) {
		other.id = id;
		other.localId = localId;
		other.helloDate = helloDate;
		other.auth = auth;
		other.userAgent = userAgent;
		return other;
	}

	@Transient
	public long getLocalId() {
		return this.localId;
	}

	public void setAuth(Integer auth) {
		this.auth = auth;
	}

	public void setHelloDate(Date helloDate) {
		this.helloDate = helloDate;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setLocalId(long localId) {
		this.localId = localId;
	}

	public String getUserAgent() {
		return this.userAgent;
	}

	public void setUserAgent(String userAgent) {
		if (userAgent != null && userAgent.length() > 200) {
			userAgent = userAgent.substring(0, 200);
		}
		this.userAgent = userAgent;
	}
}
