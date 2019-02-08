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

import com.google.gwt.user.client.rpc.GwtTransient;
import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@MappedSuperclass
@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
@Introspectable
/**
 * Important note - the subclass IUser field should be @GwtTransient - to
 * prevent accidental access of possibly different IUser objects
 * 
 * @author nick@alcina.cc
 *
 */
public abstract class ClientInstance
        implements HasIUser, HasIdAndLocalId, Serializable, Cloneable {
    private long id;

    private long localId;

    private Date helloDate;

    private Integer auth;

    private String userAgent;

    @GwtTransient
    private Boolean botUserAgent;

    @GwtTransient
    private String iid;

    private String referrer;

    private String url;

    @GwtTransient
    private String ipAddress;

    @Override
    public abstract ClientInstance clone();

    public ClientInstance copyPropertiesTo(ClientInstance other) {
        other.id = id;
        other.localId = localId;
        other.helloDate = helloDate;
        other.auth = auth;
        other.userAgent = userAgent;
        other.iid = iid;
        return other;
    }

    public Integer getAuth() {
        return auth;
    }

    public Boolean getBotUserAgent() {
        return this.botUserAgent;
    }

    public Date getHelloDate() {
        return helloDate;
    }

    @Override
    @Transient
    public long getId() {
        return id;
    }

    public String getIid() {
        return this.iid;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    @Override
    @Transient
    public long getLocalId() {
        return this.localId;
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

    @Override
    public void setLocalId(long localId) {
        this.localId = localId;
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

    public static class ClientInstanceTransportImpl extends ClientInstance {
        public static ClientInstanceTransportImpl from(
                ClientInstance persistentInstance) {
            ClientInstanceTransportImpl transportImpl = new ClientInstanceTransportImpl();
            transportImpl.setAuth(persistentInstance.getAuth());
            transportImpl.setId(persistentInstance.getId());
            transportImpl.setHelloDate(persistentInstance.getHelloDate());
            transportImpl.setIid(persistentInstance.getIid());
            transportImpl.setIpAddress(persistentInstance.getIpAddress());
            transportImpl.setReferrer(persistentInstance.getReferrer());
            transportImpl.setUrl(persistentInstance.getUrl());
            transportImpl.setUserAgent(persistentInstance.getUserAgent());
            return transportImpl;
        }

        @Override
        public ClientInstance clone() {
            return null;
        }

        @Override
        public IUser getUser() {
            return null;
        }

        @Override
        public void setUser(IUser user) {
        }
    }
}
