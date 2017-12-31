package cc.alcina.framework.jvmclient.service;

import cc.alcina.framework.gwt.persistence.client.ClientSession.ClientSessionSingleAccess;

public class ClientSessionJvm extends ClientSessionSingleAccess {
	@Override
	protected void init() {
		// no GWT calls, natch
	}
}