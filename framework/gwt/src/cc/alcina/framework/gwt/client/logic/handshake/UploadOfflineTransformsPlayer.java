package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;

public class UploadOfflineTransformsPlayer
		extends RunnableAsyncCallbackPlayer<Void, HandshakeState> {
	public static final HandshakeState OFFLINE_TRANSFORMS_UPLOADED = new HandshakeState(
			"OFFLINE_TRANSFORMS_UPLOADED");

	public UploadOfflineTransformsPlayer() {
		addRequires(HandshakeState.SERVICES_INITIALISED);
		addProvides(OFFLINE_TRANSFORMS_UPLOADED);
	}

	@Override
	public void onFailure(Throwable caught) {
		if (ClientUtils.maybeOffline(caught)) {
			// we now know that we're offline.
			wasPlayed();
			return;
		}
		super.onFailure(caught);
	}

	@Override
	public void run() {
		LocalTransformPersistence.get().handleUncommittedTransformsOnLoad(this);
	}
}
