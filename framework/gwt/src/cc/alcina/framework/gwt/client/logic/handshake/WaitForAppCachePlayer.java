package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.persistence.client.OfflineManager;

public class WaitForAppCachePlayer extends RunnableAsyncCallbackPlayer {
	public static final HandshakeState APP_CACHE_CHECKED = new HandshakeState(
			"APP_CACHE_CHECKED");

	public WaitForAppCachePlayer() {
		addRequires(HandshakeState.ASYNC_SERVICES_INITIALISED);
		addProvides(APP_CACHE_CHECKED);
	}

	@Override
	public void run() {
		OfflineManager.get().checkCacheLoading(this);
	}
}
