package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import java.util.Iterator;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.state.Consort;

public class LoadChunksFromLocalStorageConsort extends
		Consort<LoadChunksFromLocalStorageState, Object> {
	public Iterator<DomainModelDelta> chunkDeltaIterator;

	public LoadChunksFromLocalStorageConsort() {
		addPlayer(new RetrieveLocalModelChunkDeltasPlayer());
		addPlayer(new ReplayLocalModelFirstChunkDeltasToGetMergeInfoPlayer());
		addEndpointPlayer();
	}
}
