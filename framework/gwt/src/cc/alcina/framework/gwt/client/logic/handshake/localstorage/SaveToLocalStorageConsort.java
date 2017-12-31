package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import cc.alcina.framework.common.client.state.AllStatesConsort;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.localstorage.SaveToLocalStorageConsort.State;
import cc.alcina.framework.gwt.persistence.client.ClientSession;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;

/**
 * Note the logic - at most one chunk will be persisted, and at most one
 * reparented
 * 
 * 
 * REDO - instead,<br>
 * (a) get the lock before persisting to the local db (b) remove *all* remote
 * playback things (c) just write the replay deltas (e.g. domainholder x,
 * deltas/unlinked y) (d) release lock
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class SaveToLocalStorageConsort extends AllStatesConsort<State> {
	public SaveToLocalStorageConsort() {
		super(State.class, null);
	}

	@Override
	public void runPlayer(AllStatesPlayer player, State next) {
		switch (next) {
		case ACQUIRE_CROSS_TAB_PERSISTENCE_LOCK:
			ClientSession.get().acquireCrossTabPersistenceLock(player);
			break;
		case CLEAR_UNNEEDED_PLAYBACK_DATA:
			LocalTransformPersistence.get().clearPersistedClient(null, 0,
					player, false);
			break;
		case SAVE_CHUNKS:
			LocalTransformPersistence.get().persist(HandshakeConsortModel.get()
					.getPersistableApplicationRecords(), player);
			break;
		case RELEASE_CROSS_TAB_PERSISTENCE_LOCK:
			ClientSession.get().releaseCrossTabPersistenceLock();
			break;
		}
	}

	static enum State {
		ACQUIRE_CROSS_TAB_PERSISTENCE_LOCK, CLEAR_UNNEEDED_PLAYBACK_DATA,
		SAVE_CHUNKS, RELEASE_CROSS_TAB_PERSISTENCE_LOCK
	}
}
