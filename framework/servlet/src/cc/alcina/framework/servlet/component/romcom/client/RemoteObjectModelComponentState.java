package cc.alcina.framework.servlet.component.romcom.client;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class RemoteObjectModelComponentState {
	public static RemoteObjectModelComponentState get() {
		return Registry.impl(RemoteObjectModelComponentState.class);
	}

	public boolean finished;
}
