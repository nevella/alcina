package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import java.util.Iterator;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.LoopingPlayer;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;

public class ReplayLocalModelFirstChunkDeltasToGetMergeInfoPlayer extends
		RunnableAsyncCallbackPlayer<Void, LoadChunksFromLocalStorageState>
		implements LoopingPlayer {
	private DomainModelDelta current;

	public ReplayLocalModelFirstChunkDeltasToGetMergeInfoPlayer() {
		addRequires(LoadChunksFromLocalStorageState.LOCAL_MODEL_CHUNK_DELTAS_RETRIEVED);
		addProvides(LoadChunksFromLocalStorageState.LOCAL_MODEL_FIRST_CHUNK_DELTA_REPLAYED);
	}

	@Override
	public void onSuccess(Void result) {
		Registry.impl(HandshakeConsortModel.class).modelDeltas.mergeDelta(
				current, null);
		consort.replay(this);
	}

	@Override
	public void onFailure(Throwable caught) {
		Registry.impl(ClientNotifications.class).log(caught.getMessage());
		wasPlayed();
	}

	@Override
	public void loop() {
		Iterator<DomainModelDelta> deltaIterator = ((LoadChunksFromLocalStorageConsort) getConsort()).chunkDeltaIterator;
		if (deltaIterator.hasNext()) {
			current = deltaIterator.next();
			current.unwrap(this);
		} else {
			wasPlayed();
		}
	}

	@Override
	public void run() {
		loop();
	}

	public String describeLoop() {
		return "loops on chunk deltas (at most 2) - checks metadata and may eval() asynchronously";
	}
}