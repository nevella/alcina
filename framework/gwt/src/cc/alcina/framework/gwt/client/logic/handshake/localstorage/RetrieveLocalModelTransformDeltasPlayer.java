package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import java.util.Iterator;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeState;
import cc.alcina.framework.gwt.client.logic.handshake.objectdata.LoadObjectDataState;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;

public class RetrieveLocalModelTransformDeltasPlayer
		extends
		RunnableAsyncCallbackPlayer<Iterator<DomainModelDelta>, LoadObjectDataState> {
	public RetrieveLocalModelTransformDeltasPlayer() {
		addProvides(LoadObjectDataState.LOADED_TRANSFORMS_FROM_LOCAL_STORAGE);
		addRequires(
				LoadObjectDataState.HELLO_OFFLINE_REQUIRES_PER_CLIENT_INSTANCE_TRANSFORMS,
				LoadObjectDataState.SOLE_OPEN_TAB_CHECKED);
	}

	protected DomainTransformRequestType[] getTypes() {
		return new DomainTransformRequestType[] {
				DomainTransformRequestType.TO_REMOTE,
				DomainTransformRequestType.CLIENT_SYNC,
				DomainTransformRequestType.TO_REMOTE_COMPLETED };
	}

	@Override
	public void onSuccess(Iterator<DomainModelDelta> result) {
		Registry.impl(HandshakeConsortModel.class).modelDeltas.transformDeltaIterator = result;
		super.onSuccess(result);
	}

	@Override
	public void run() {
		LocalTransformPersistence.get().getDomainModelDeltaIterator(getTypes(),
				this);
	}
}
