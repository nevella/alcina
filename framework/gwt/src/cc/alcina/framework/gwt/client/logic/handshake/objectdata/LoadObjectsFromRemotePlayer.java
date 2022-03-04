package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.csobjects.LoadObjectsResponse;
import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;

@Reflected
@Registration.Singleton
public abstract class LoadObjectsFromRemotePlayer extends
		RunnableAsyncCallbackPlayer<LoadObjectsResponse, LoadObjectDataState> {
	public LoadObjectsFromRemotePlayer() {
		addRequires(LoadObjectDataState.HELLO_OK_REQUIRES_OBJECT_DATA_UPDATE);
		addProvides(LoadObjectDataState.OBJECT_DATA_LOADED);
	}

	@Override
	public void onSuccess(LoadObjectsResponse result) {
		HandshakeConsortModel.get().setLoadObjectsResponse(result);
		super.onSuccess(result);
	}

	@Override
	public void run() {
		LoadObjectsRequest request = Registry.impl(HandshakeConsortModel.class)
				.getLoadObjectsRequest();
		loadObjects(request);
	}

	protected abstract void loadObjects(LoadObjectsRequest request);
}
