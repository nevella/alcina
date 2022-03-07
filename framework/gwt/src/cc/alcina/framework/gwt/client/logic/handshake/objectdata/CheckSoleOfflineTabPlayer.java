package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.logic.handshake.CheckSoleInstancePlayer.NotSoleInstanceException;
import cc.alcina.framework.gwt.persistence.client.ClientSession;

@Reflected
@Registration.Singleton
public class CheckSoleOfflineTabPlayer
		extends RunnableAsyncCallbackPlayer<Boolean, LoadObjectDataState> {
	public CheckSoleOfflineTabPlayer() {
		addProvides(LoadObjectDataState.SOLE_OPEN_TAB_CHECKED);
		addRequires(LoadObjectDataState.HELLO_OFFLINE);
	}

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

	protected void checkFailed() {
		Registry.impl(ClientNotifications.class)
				.getModalNotifier("Only one offline tab permitted");
	}
}
