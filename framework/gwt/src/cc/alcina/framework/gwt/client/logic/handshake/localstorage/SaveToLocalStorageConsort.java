package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.EnumPlayer;
import cc.alcina.framework.common.client.state.EnumPlayer.EnumRunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.localstorage.SaveToLocalStorageConsort.State;
import cc.alcina.framework.gwt.persistence.client.ClientSession;
import cc.alcina.framework.gwt.persistence.client.DtrWrapperBackedDomainModelDelta;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;

/**
 * Note the logic - at most one chunk will be persisted, and at most one
 * reparented
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class SaveToLocalStorageConsort extends Consort<State, Object> {
	static enum State {
		ACQUIRE_CROSS_TAB_PERSISTENCE_LOCK, CLEAR_UNNEEDED_PLAYBACK_DATA,
		MAYBE_SAVE_CHUNK_1_OR_2, RELEASE_CROSS_TAB_PERSISTENCE_LOCK
	}

	HandshakeConsortModel handshakeModel = Registry
			.impl(HandshakeConsortModel.class);

	public SaveToLocalStorageConsort() {
		addPlayer(new AcquireCrossTabPersistenceLock());
		addPlayer(new ClearUnneededPlaybackData());
		addPlayer(new MaybeSaveChunk1or2());
		addPlayer(new ReleaseCrossTabPersistenceLock());
		addEndpointPlayer();
	}

	class AcquireCrossTabPersistenceLock extends
			EnumRunnableAsyncCallbackPlayer<Void, State> {
		public AcquireCrossTabPersistenceLock() {
			super(State.ACQUIRE_CROSS_TAB_PERSISTENCE_LOCK);
		}

		@Override
		public void run() {
			ClientSession.get().acquireCrossTabPersistenceLock(this);
		}
	}

	class ReleaseCrossTabPersistenceLock extends EnumPlayer<State> {
		public ReleaseCrossTabPersistenceLock() {
			super(State.RELEASE_CROSS_TAB_PERSISTENCE_LOCK);
		}

		@Override
		public void run() {
			ClientSession.get().releaseCrossTabPersistenceLock();
		}
	}

	class ClearUnneededPlaybackData extends
			EnumRunnableAsyncCallbackPlayer<Void, State> {
		public ClearUnneededPlaybackData() {
			super(State.CLEAR_UNNEEDED_PLAYBACK_DATA);
		}

		@Override
		public void run() {
			int exceptForId = 0;
			DomainModelDelta firstChunk = handshakeModel.modelDeltas.firstChunk;
			if (firstChunk != null
					&& handshakeModel.modelDeltas.payloads.get(firstChunk) == null) {
				exceptForId = ((DtrWrapperBackedDomainModelDelta) firstChunk)
						.getWrapper().getId();
			}
			LocalTransformPersistence.get().clearPersistedClient(
					handshakeModel.getClientInstance(), exceptForId, this);
		}
	}

	class MaybeSaveChunk1or2 extends
			EnumRunnableAsyncCallbackPlayer<Void, State> {
		public MaybeSaveChunk1or2() {
			super(State.MAYBE_SAVE_CHUNK_1_OR_2);
		}

		@Override
		public void run() {
			long exceptForId = 0;
			DomainModelDelta firstChunk = handshakeModel.modelDeltas.firstChunk;
			DomainModelDelta secondChunk = handshakeModel.modelDeltas.secondChunk;
			if (firstChunk != null
					&& handshakeModel.modelDeltas.payloads.get(firstChunk) != null) {
				LocalTransformPersistence.get().persistInitialRpcPayload(
						handshakeModel.modelDeltas.payloads.get(firstChunk),
						this);
				System.out.println("save (1) - "
						+ handshakeModel.modelDeltas.payloads.get(firstChunk)
								.length());
			} else if (secondChunk != null
					&& handshakeModel.modelDeltas.payloads.get(secondChunk) != null) {
				LocalTransformPersistence.get().persistInitialRpcPayload(
						handshakeModel.modelDeltas.payloads.get(secondChunk),
						this);
				System.out.println("save (2) - "
						+ handshakeModel.modelDeltas.payloads.get(secondChunk)
								.length());
			} else {
				wasPlayed();
			}
		}
	}
}
