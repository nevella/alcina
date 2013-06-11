package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.gwt.core.client.GWT;

@RegistryLocation(registryPoint = HandshakeConsortModel.class, implementationType = ImplementationType.SINGLETON)
public class HandshakeConsortModel {
	private LoginResponse loginResponse;

	private ClientInstance clientInstance;

	public LoadObjectsHolder loadObjectsHolder;

	public Long lastTransformId;

	public LoadObjectsRequest getLoadObjectsRequest() {
		LoadObjectsRequest request = new LoadObjectsRequest();
		request.setLastTransformId(lastTransformId);
		request.setTypeSignature(GWT.getPermutationStrongName());
		return request;
	}

	public void clearObjects() {
		lastTransformId = null;
		loadObjectsHolder = null;
	}

	public ModalNotifier loadObjectsNotifier;

	public ModalNotifier ensureLoadObjectsNotifier(String message) {
		if (loadObjectsNotifier == null) {
			loadObjectsNotifier = Registry.impl(ClientNotifications.class)
					.getModalNotifier(message);
		} else {
			loadObjectsNotifier.setStatus(message);
		}
		return loadObjectsNotifier;
	}

	public void clearLoadObjectsNotifier() {
		if (loadObjectsNotifier != null) {
			loadObjectsNotifier.modalOff();
		}
	}

	public LoginState getLoginState() {
		ClientInstance clientInstance = Registry.impl(ClientInstance.class);
		if (clientInstance == null) {
			return LoginState.NOT_LOGGED_IN;
		}
		return PermissionsManager.getAnonymousUserName().equals(
				PermissionsManager.get().getUser().getUserName()) ? LoginState.NOT_LOGGED_IN
				: LoginState.LOGGED_IN;
	}

	public LoginResponse getLoginResponse() {
		return loginResponse;
	}

	public void setLoginResponse(LoginResponse loginResponse) {
		this.loginResponse = loginResponse;
		if (loginResponse != null) {
			setClientInstance(loginResponse.getClientInstance());
			
		}
	}

	public ClientInstance getClientInstance() {
		return this.clientInstance;
	}

	public void setClientInstance(ClientInstance clientInstance) {
		Registry.putSingleton(ClientInstance.class, clientInstance);
		this.clientInstance = clientInstance;
	}
}
