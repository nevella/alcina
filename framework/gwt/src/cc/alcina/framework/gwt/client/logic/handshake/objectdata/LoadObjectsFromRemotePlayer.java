package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder.AlcinaRpcRequestBuilderCreationOneOffReplayableListener;

@RegistryLocation(registryPoint=LoadObjectsFromRemotePlayer.class,implementationType=ImplementationType.SINGLETON)
@ClientInstantiable
public abstract class LoadObjectsFromRemotePlayer
		extends
		RunnableAsyncCallbackPlayer<LoadObjectsHolder, LoadObjectDataState> {
	private AlcinaRpcRequestBuilderCreationOneOffReplayableListener builderListener;

	public LoadObjectsFromRemotePlayer() {
		addRequires(LoadObjectDataState.HELLO_OK_REQUIRES_OBJECT_DATA_UPDATE);
		addProvides(LoadObjectDataState.OBJECT_DATA_LOADED);
	}

	@Override
	public void run() {
		LoadObjectsRequest request = Registry.impl(HandshakeConsortModel.class)
				.getLoadObjectsRequest();
		new AlcinaRpcRequestBuilderCreationOneOffReplayableListener();
		this.builderListener = AlcinaRpcRequestBuilder
				.addOneoffReplayableCreationListener();
		loadObjects(request);
	}

	protected abstract void loadObjects(LoadObjectsRequest request);

	@Override
	public void onSuccess(LoadObjectsHolder result) {
		Registry.impl(HandshakeConsortModel.class).modelDeltas.mergeDelta(
				result, builderListener.builder.getRpcResult());
		super.onSuccess(result);
	}
}