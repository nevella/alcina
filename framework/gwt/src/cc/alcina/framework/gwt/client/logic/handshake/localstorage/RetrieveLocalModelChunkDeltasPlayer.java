package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import java.util.Iterator;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;

public class RetrieveLocalModelChunkDeltasPlayer
		extends
		RunnableAsyncCallbackPlayer<Iterator<DomainModelDelta>, LoadChunksFromLocalStorageState> {
	public RetrieveLocalModelChunkDeltasPlayer() {
		addProvides(LoadChunksFromLocalStorageState.LOCAL_MODEL_CHUNK_DELTAS_RETRIEVED);
	}

	protected DomainTransformRequestType[] getTypes() {
		return new DomainTransformRequestType[] { DomainTransformRequestType.CLIENT_OBJECT_LOAD };
	}

	@Override
	public void onSuccess(Iterator<DomainModelDelta> result) {
		((LoadChunksFromLocalStorageConsort)getConsort()).chunkDeltaIterator = result;
		super.onSuccess(result);
	}

	@Override
	public void run() {
		LocalTransformPersistence.get().getDomainModelDeltaIterator(getTypes(),
				this);
	}
}
