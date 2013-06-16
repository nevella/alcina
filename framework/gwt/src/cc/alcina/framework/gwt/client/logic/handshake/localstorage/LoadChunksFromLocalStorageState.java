package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

public class LoadChunksFromLocalStorageState extends ExtensibleEnum {
	public LoadChunksFromLocalStorageState(String key) {
		super(key);
	}

	public static final LoadChunksFromLocalStorageState LOCAL_MODEL_CHUNK_DELTAS_RETRIEVED = new LoadChunksFromLocalStorageState(
			"LOCAL_MODEL_CHUNK_DELTAS_RETRIEVED");

	public static final LoadChunksFromLocalStorageState LOCAL_MODEL_FIRST_CHUNK_DELTA_REPLAYED = new LoadChunksFromLocalStorageState(
			"LOCAL_MODEL_FIRST_CHUNK_DELTA_REPLAYED");

	// we need this two-phase approach because we may need the client instance
	// id to determine which transform deltas to replay
	public static final LoadChunksFromLocalStorageState LOCAL_MODEL_TRANSFORM_DELTAS_RETRIEVED = new LoadChunksFromLocalStorageState(
			"LOCAL_MODEL_TRANSFORM_DELTAS_RETRIEVED");

	public static final LoadChunksFromLocalStorageState POST_REPLAY_COMPLETED = new LoadChunksFromLocalStorageState(
			"POST_REPLAY_COMPLETED");
}
