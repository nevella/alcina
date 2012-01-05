package cc.alcina.framework.gwt.persistence.client;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientMetricLogging;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeHelper;

public abstract class ClientHandshakeHelperWithLocalPersistence extends
		ClientHandshakeHelper {
	public abstract boolean supportsRpcPersistence();

	protected MixedGwtTransformHelper mixedHelper;

	@Override
	protected void locallyPersistDomainModelAndReplayPostLoadTransforms(
			final LoginState loginState, final ScheduledCommand postRegisterCommand) {
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
						if (mixedHelper.isUseMixedObjectLoadSequence()) {
							// synchronous
							ClientMetricLogging.get()
									.start("replay-transforms");
							TransformManager.get()
									.setReplayingRemoteEvent(true);
							TransformManager.get().replayRemoteEvents(
									mixedHelper.getHolder().getReplayEvents(),
									false);
							TransformManager.get().setReplayingRemoteEvent(
									false);
							ClientMetricLogging.get().end("replay-transforms");
						} else {
							ClientMetricLogging.get().start("persist-rpc");
							PersistenceCallback<Void> callback=new PersistenceCallback<Void>(){

								@Override
								public void onFailure(Throwable caught) {
									cleanup();
									ClientSession.get().setInitialObjectsPersisted(false);
									throw new WrappedRuntimeException(caught);
								}

								@Override
								public void onSuccess(Void result) {
									cleanup();
									afterLocalPersistenceAndReplay(loginState,postRegisterCommand);
									
								}
								void cleanup(){
									ClientMetricLogging.get().end("persist-rpc");
								}
								
							};
							mixedHelper.persistGwtObjectGraph(callback);
							return;
							
						}
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
		afterLocalPersistenceAndReplay(loginState,postRegisterCommand);
	}

	protected abstract ClientInstance createClientInstance(
			long clientInstanceId, int clientInstanceAuth);

	public abstract SerializedDomainLoader getSerializedDomainLoader();

	protected abstract void preSerialization(LoginState loginState);

	/**
	 * Override for gwt deserialization, and call new MixedGwtTransformHelper()
	 * .replayRpc
	 */
	public LoadObjectsHolder replayRpc(String text) {
		throw new UnsupportedOperationException();
	}
}
