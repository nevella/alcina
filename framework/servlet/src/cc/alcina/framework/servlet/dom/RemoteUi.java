package cc.alcina.framework.servlet.dom;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.Client;

public interface RemoteUi {
	default Client createClient() {
		return Registry.impl(ClientRemoteImpl.class);
	}

	void init();

	void render();
}
