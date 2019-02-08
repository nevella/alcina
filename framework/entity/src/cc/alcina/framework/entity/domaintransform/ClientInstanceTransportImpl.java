package cc.alcina.framework.entity.domaintransform;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IUser;

public class ClientInstanceTransportImpl extends ClientInstance {
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