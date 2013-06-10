package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.gwt.core.client.GWT;

@RegistryLocation(registryPoint = HandshakeConsortModel.class, implementationType = ImplementationType.SINGLETON)
public class HandshakeConsortModel {
	public  LoginState loginState;

	public LoginResponse helloResponse;

	public ClientInstance getClientInstance() {
		return helloResponse == null ? null : helloResponse.getClientInstance();
	}

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

	public ModalNotifier ensureModalNotifier(String message) {
		return Registry.impl(ClientNotifications.class).getModalNotifier(
				message);
	}

	public void clearModalNotifier() {
		Registry.impl(ClientNotifications.class).getModalNotifier("");
	}
}
