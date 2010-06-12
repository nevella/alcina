package cc.alcina.framework.gwt.client.logic;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientNofications;
import cc.alcina.framework.gwt.gears.client.ClientSession;
import cc.alcina.framework.gwt.gears.client.LocalTransformPersistence;

public abstract class ClientHandshakeHelperWithLocalPersistence extends
		ClientHandshakeHelper {
	@Override
	protected void locallyPersistDomainModel(LoginState loginState) {
		LocalTransformPersistence localPersistence = LocalTransformPersistence
				.get();
		try {
			if (localPersistence != null
					&& localPersistence.isLocalStorageInstalled()
					&& localPersistence.shouldPersistClient()
					&& loginState == LoginState.LOGGED_IN) {
				// set before
				ClientSession.get().setInitialObjectsPersisted(true);
				preSerialization(loginState);
				ClientTransformManager.cast().serializeDomainObjects(
						ClientLayerLocator.get().getClientInstance());
			}
		} catch (Exception e) {
			ClientSession.get().setInitialObjectsPersisted(false);
			throw new WrappedRuntimeException(e);
		}
	}

	protected abstract void preSerialization(LoginState loginState);
}
