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
	protected HandshakeConsortModel handshakeConsortModel = Registry
			.impl(HandshakeConsortModel.class);

	public LoadObjectsHelloPlayer() {
		addProvides(helloOkState());
		addProvides(LoadObjectDataState.HELLO_OFFLINE);
		addProvides(LoadObjectDataState.OBJECT_DATA_LOAD_FAILED);
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

	protected boolean allowAnonymousObjectLoad() {
		return true;
	}

	protected void handleLoginResponse(LoginResponse loginResponse) {
		signal(loginResponse.isOk() || allowAnonymousObjectLoad());
	}

	protected void hello() {
		Registry.impl(CommonRemoteServiceAsync.class).hello(this);
	}

	protected LoadObjectDataState helloOkState() {
		return LoadObjectDataState.HELLO_OK_REQUIRES_OBJECT_DATA_UPDATE;
	}

	/*
	 * call logic is a bit fraught here - it works, but ain't pretty
	 */
	protected void signal(boolean continueObjectLoad) {
		if (PermissionsManager.isOffline()) {
			wasPlayed(LoadObjectDataState.HELLO_OFFLINE);
		} else {
			if (continueObjectLoad) {
				wasPlayed(helloOkState());
			} else {
				wasPlayed(LoadObjectDataState.OBJECT_DATA_LOAD_FAILED);
			}
		}
	}
}