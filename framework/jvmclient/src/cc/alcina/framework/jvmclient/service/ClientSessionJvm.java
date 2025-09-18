package cc.alcina.framework.jvmclient.service;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.gwt.persistence.client.ClientSession;
import cc.alcina.framework.gwt.persistence.client.ClientSession.ClientSessionSingleAccess;
@Registration(
		value = ClientSession.class,
		priority = Registration.Priority.REMOVE)
public class ClientSessionJvm extends ClientSessionSingleAccess {
	@Override
	protected void init() {
		// no GWT calls, natch
	}
}