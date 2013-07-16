package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.logic.handshake.CheckSoleInstancePlayer.NotSoleInstanceException;
import cc.alcina.framework.gwt.persistence.client.ClientSession;

@RegistryLocation(registryPoint=CheckSoleOfflineTabPlayer.class,implementationType=ImplementationType.SINGLETON)
@ClientInstantiable
public class CheckSoleOfflineTabPlayer
		extends
		RunnableAsyncCallbackPlayer<Boolean, LoadObjectDataState> {
	public CheckSoleOfflineTabPlayer() {
		addProvides(LoadObjectDataState.SOLE_OPEN_TAB_CHECKED);
		addRequires(LoadObjectDataState.HELLO_OFFLINE_REQUIRES_PER_CLIENT_INSTANCE_TRANSFORMS);
	}


	@Override
	public void run() {
		ClientSession.get().checkSoleOpenTab(this);
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

	protected  void checkFailed(){
		Registry.impl(ClientNotifications.class).getModalNotifier("Only one offline tab permitted");
	}
}
