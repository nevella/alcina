package cc.alcina.extras.dev.console.remote.server;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration(RemoteInvocationServlet_DevConsole_Customiser.class)
public class RemoteInvocationServlet_DevConsole_Customiser {
	public static RemoteInvocationServlet_DevConsole_Customiser get() {
		return Registry
				.impl(RemoteInvocationServlet_DevConsole_Customiser.class);
	}

	protected void customiseContextBeforePayloadWrite() {
	}
}
