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

import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
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
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
public abstract class ClientInstance extends VersionableEntity<ClientInstance> {
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

	@GwtTransient
	/*
	 * This duplicates authenticationSession.user.id - but is needed for search
	 * completeness (since older clientinstances have no auth session)
	 */
	private Long user_id;

	public Integer getAuth() {
		return auth;
	}

	@Transient
	public abstract AuthenticationSession getAuthenticationSession();

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

	@Transient
	public abstract ClientInstance getReplaces();

	public String getUrl() {
		return this.url;
	}

	public Long getUser_id() {
		return this.user_id;
	}

	public String getUserAgent() {
		return this.userAgent;
	}

	public IUser provideUser() {
		return Optional.ofNullable(getAuthenticationSession())
				.map(AuthenticationSession::getUser).orElse(null);
	}

	public void setAuth(Integer auth) {
		Integer old_auth = this.auth;
		this.auth = auth;
		propertyChangeSupport().firePropertyChange("auth", old_auth, auth);
	}

	public abstract void setAuthenticationSession(
			AuthenticationSession authenticationSession);

	public void setBotUserAgent(Boolean botUserAgent) {
		Boolean old_botUserAgent = this.botUserAgent;
		this.botUserAgent = botUserAgent;
		propertyChangeSupport().firePropertyChange("botUserAgent",
				old_botUserAgent, botUserAgent);
	}

	public void setExpired(Boolean expired) {
		Boolean old_expired = this.expired;
		this.expired = expired;
		propertyChangeSupport().firePropertyChange("expired", old_expired,
				expired);
	}

	public void setHelloDate(Date helloDate) {
		Date old_helloDate = this.helloDate;
		this.helloDate = helloDate;
		propertyChangeSupport().firePropertyChange("helloDate", old_helloDate,
				helloDate);
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setIid(String iid) {
		String old_iid = this.iid;
		this.iid = iid;
		propertyChangeSupport().firePropertyChange("iid", old_iid, iid);
	}

	public void setIpAddress(String ipAddress) {
		String old_ipAddress = this.ipAddress;
		this.ipAddress = ipAddress;
		propertyChangeSupport().firePropertyChange("ipAddress", old_ipAddress,
				ipAddress);
	}

	public void setLastAccessed(Date lastAccessed) {
		Date old_lastAccessed = this.lastAccessed;
		this.lastAccessed = lastAccessed;
		propertyChangeSupport().firePropertyChange("lastAccessed",
				old_lastAccessed, lastAccessed);
	}

	public void setReferrer(String referrer) {
		String old_referrer = this.referrer;
		this.referrer = referrer;
		propertyChangeSupport().firePropertyChange("referrer", old_referrer,
				referrer);
	}

	public abstract void setReplaces(ClientInstance replaces);

	public void setUrl(String url) {
		String old_url = this.url;
		this.url = url;
		propertyChangeSupport().firePropertyChange("url", old_url, url);
	}

	public void setUser_id(Long user_id) {
		Long old_user_id = this.user_id;
		this.user_id = user_id;
		propertyChangeSupport().firePropertyChange("user_id", old_user_id,
				user_id);
	}

	public void setUserAgent(String userAgent) {
		if (userAgent != null && userAgent.length() > 200) {
			userAgent = userAgent.substring(0, 200);
		}
		String old_userAgent = this.userAgent;
		this.userAgent = userAgent;
		propertyChangeSupport().firePropertyChange("userAgent", old_userAgent,
				userAgent);
	}
}
