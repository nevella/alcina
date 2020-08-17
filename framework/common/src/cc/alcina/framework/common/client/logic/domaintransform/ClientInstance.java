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

import java.util.Date;
import java.util.Optional;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.Bean;

@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */
@Bean
/**
 * 
 * @author nick@alcina.cc
 * 
 */
public abstract class ClientInstance extends Entity<ClientInstance> {
	private Date helloDate;

	private Integer auth;

	@GwtTransient
	private String userAgent;

	@GwtTransient
	private Boolean botUserAgent;

	@GwtTransient
	private String iid;

	@GwtTransient
	private String referrer;

	@GwtTransient
	private String url;

	@GwtTransient
	private String ipAddress;

	@GwtTransient
	private Date lastAccessed;

	@GwtTransient
	private Boolean expired;

	@Transient
	public abstract AuthenticationSession getAuthenticationSession();

	public abstract void setAuthenticationSession(
			AuthenticationSession authenticationSession);

	@Transient
	public abstract ClientInstance getReplaces();

	public abstract void setReplaces(ClientInstance replaces);

	public Integer getAuth() {
		return auth;
	}

	public Boolean getBotUserAgent() {
		return this.botUserAgent;
	}

	public Boolean getExpired() {
		return this.expired;
	}

	public Date getHelloDate() {
		return helloDate;
	}

	@Override
	@Transient
	public long getId() {
		return id;
	}

	/*
	 * FIXME - mvcc.5 - remove (once mvcc.auth has settled)
	 */
	public String getIid() {
		return this.iid;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public Date getLastAccessed() {
		return this.lastAccessed;
	}

	public String getReferrer() {
		return this.referrer;
	}

	public String getUrl() {
		return this.url;
	}

	public String getUserAgent() {
		return this.userAgent;
	}

	public void setAuth(Integer auth) {
		this.auth = auth;
	}

	public void setBotUserAgent(Boolean botUserAgent) {
		this.botUserAgent = botUserAgent;
	}

	public void setExpired(Boolean expired) {
		this.expired = expired;
	}

	public void setHelloDate(Date helloDate) {
		this.helloDate = helloDate;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setIid(String iid) {
		this.iid = iid;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public void setLastAccessed(Date lastAccessed) {
		this.lastAccessed = lastAccessed;
	}

	public void setReferrer(String referrer) {
		this.referrer = referrer;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUserAgent(String userAgent) {
		if (userAgent != null && userAgent.length() > 200) {
			userAgent = userAgent.substring(0, 200);
		}
		this.userAgent = userAgent;
	}

	public IUser provideUser() {
		return Optional.ofNullable(getAuthenticationSession())
				.map(AuthenticationSession::getUser).orElse(null);
	}
}
