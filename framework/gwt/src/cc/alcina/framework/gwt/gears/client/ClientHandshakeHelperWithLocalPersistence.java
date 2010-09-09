package cc.alcina.framework.gwt.gears.client;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientMetricLogging;
import cc.alcina.framework.gwt.client.ClientNofications;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeHelper;

public abstract class ClientHandshakeHelperWithLocalPersistence extends
		ClientHandshakeHelper {
	public abstract boolean supportsRpcPersistence();

	protected MixedGwtTransformHelper mixedHelper;

	@Override
	protected void locallyPersistDomainModelAndReplayPostLoadTransforms(
			LoginState loginState) {
		LocalTransformPersistence localPersistence = LocalTransformPersistence
				.get();
		try {
			if (localPersistence != null
					&& localPersistence.isLocalStorageInstalled()
					&& localPersistence
							.shouldPersistClient(supportsRpcPersistence())
					&& loginState == LoginState.LOGGED_IN) {
				// set before
				ClientSession.get().setInitialObjectsPersisted(true);
				if (supportsRpcPersistence()) {
					if (mixedHelper != null) {
						ClientMetricLogging.get().start("persist-rpc");
						mixedHelper.persistGwtObjectGraph();
						ClientMetricLogging.get().end("persist-rpc");
						mixedHelper = null;
					}
				} else {
					preSerialization(loginState);
					ClientTransformManager.cast().serializeDomainObjects(
							ClientLayerLocator.get().getClientInstance());
				}
			}
		} catch (Exception e) {
			ClientSession.get().setInitialObjectsPersisted(false);
			throw new WrappedRuntimeException(e);
		}
	}

	protected abstract void preSerialization(LoginState loginState);

	/**
	 * Override for gwt deserialization, and call new MixedGwtTransformHelper()
	 * .replayRpc
	 */
	public LoadObjectsHolder replayRpc(String text) {
		throw new UnsupportedOperationException();
	}
}
