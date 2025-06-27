package cc.alcina.framework.entity.logic;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;

/**
 * A holder for the server client instance (just because of the wordiness of
 * ServerClientInstance.get())
 */
public class ServerClientInstance {
	public static ClientInstance get() {
		return EntityLayerObjects.get().getServerAsClientInstance();
	}
}
