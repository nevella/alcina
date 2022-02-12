package cc.alcina.extras.dev.console.remote.server;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@RegistryLocation(registryPoint = RemoteInvocationServlet_DevConsole_Customiser.class, implementationType = ImplementationType.INSTANCE)
public class RemoteInvocationServlet_DevConsole_Customiser {
	public static RemoteInvocationServlet_DevConsole_Customiser get() {
		return Registry
				.impl(RemoteInvocationServlet_DevConsole_Customiser.class);
	}

	protected void customiseContextBeforePayloadWrite() {
	}
}