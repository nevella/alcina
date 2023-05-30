package cc.alcina.extras.dev.component.remote.client;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class RemoteComponentState {
	public static RemoteComponentState get() {
		return Registry.impl(RemoteComponentState.class);
	}

	public boolean finished;
}
