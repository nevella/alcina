package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.util.ClientUtils;

@RegistryLocation(registryPoint = LoadObjectsHelloPlayer.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class LoadObjectsHelloPlayer extends
		RunnableAsyncCallbackPlayer<LoginResponse, LoadObjectDataState> {
	public LoadObjectsHelloPlayer() {
		addProvides(helloOkState());
		addProvides(LoadObjectDataState.HELLO_OFFLINE_REQUIRES_PER_CLIENT_INSTANCE_TRANSFORMS);
		addProvides(LoadObjectDataState.OBJECT_DATA_LOAD_FAILED);
	}

	HandshakeConsortModel handshakeConsortModel = Registry
			.impl(HandshakeConsortModel.class);

	@Override
	public void run() {
		if (PermissionsManager.isOffline()) {
			signal(false);
			return;
		}
		LoginResponse existingLoginResponse = handshakeConsortModel
				.getLoginResponse();
		if (existingLoginResponse == null) {
			hello();
		} else {
			handleLoginResponse(existingLoginResponse);
		}
	}

	protected void hello() {
		Registry.impl(CommonRemoteServiceAsync.class).hello(this);
	}

	void signal(boolean helloOk) {
		if (handshakeConsortModel.haveAllChunksNeededForOptimalObjectLoad()) {
			handshakeConsortModel.ensureClientInstanceFromModelDeltas();
			wasPlayed(LoadObjectDataState.HELLO_OFFLINE_REQUIRES_PER_CLIENT_INSTANCE_TRANSFORMS);
		} else {
			if (helloOk) {
				wasPlayed(helloOkState());
			} else {
				wasPlayed(LoadObjectDataState.OBJECT_DATA_LOAD_FAILED);
			}
		}
	}

	protected LoadObjectDataState helloOkState() {
		return LoadObjectDataState.HELLO_OK_REQUIRES_OBJECT_DATA_UPDATE;
	}

	@Override
	public void onFailure(Throwable caught) {
		if (ClientUtils.maybeOffline(caught)) {
			PermissionsManager.get().setOnlineState(OnlineState.OFFLINE);
			signal(false);
			return;
		}
		super.onFailure(caught);
	}

	@Override
	public void onSuccess(LoginResponse loginResponse) {
		handshakeConsortModel.setLoginResponse(loginResponse);
		handleLoginResponse(loginResponse);
	}

	private void handleLoginResponse(LoginResponse loginResponse) {
		signal(loginResponse.isOk());
	}
}