package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.consort.Player.RunnableAsyncCallbackPlayer;

public class WaitForAppCachePlayer extends RunnableAsyncCallbackPlayer {
	public static final HandshakeState APP_CACHE_CHECKED = new HandshakeState(
			"APP_CACHE_CHECKED");

	public WaitForAppCachePlayer() {
		addRequires(HandshakeState.ASYNC_SERVICES_INITIALISED);
		addProvides(APP_CACHE_CHECKED);
	}

	@Override
	public void run() {
		/*
		 * appcache not supported in modern browsers
		 */
	}
}
