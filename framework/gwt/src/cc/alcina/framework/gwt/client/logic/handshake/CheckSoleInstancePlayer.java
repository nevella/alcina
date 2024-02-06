package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.consort.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.persistence.client.ClientSession;

public abstract class CheckSoleInstancePlayer
		extends RunnableAsyncCallbackPlayer<Boolean, HandshakeState> {
	public CheckSoleInstancePlayer() {
		addProvides(HandshakeState.SOLE_OPEN_TAB_CHECKED);
	}

	protected abstract void checkFailed();

	@Override
	public void onSuccess(Boolean result) {
		if (result) {
			super.onSuccess(result);
		} else {
			checkFailed();
			consort.onFailure(new NotSoleInstanceException());
		}
	}

	@Override
	public void run() {
		ClientSession.get().checkSoleOpenTab(this);
	}

	public static class NotSoleInstanceException extends Exception {
	}
}