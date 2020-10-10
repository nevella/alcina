package cc.alcina.framework.entity.transform;

import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;

public class ClientInstanceTransportImpl extends ClientInstance {
	public static ClientInstanceTransportImpl
			from(ClientInstance persistentInstance) {
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
	public AuthenticationSession getAuthenticationSession() {
		return null;
	}

	@Override
	public void setAuthenticationSession(
			AuthenticationSession authenticationSession) {
	}

	@Override
	public ClientInstance getReplaces() {
		return null;
	}

	@Override
	public void setReplaces(ClientInstance replaces) {
	}
}