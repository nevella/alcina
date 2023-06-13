package cc.alcina.framework.servlet.component.romcom.server;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration(RemoteInvocationServlet_RemoteComponent_Customiser.class)
public class RemoteInvocationServlet_RemoteComponent_Customiser {
	public static RemoteInvocationServlet_RemoteComponent_Customiser get() {
		return Registry
				.impl(RemoteInvocationServlet_RemoteComponent_Customiser.class);
	}

	protected void customiseContextBeforePayloadWrite() {
	}
}
