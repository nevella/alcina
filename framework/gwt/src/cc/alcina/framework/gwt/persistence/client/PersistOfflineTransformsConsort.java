package cc.alcina.framework.gwt.persistence.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.EnumPlayer.EnumRunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.gwt.persistence.client.PersistOfflineTransformsConsort.State;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class PersistOfflineTransformsConsort extends Consort<State, Object> {
	public static enum State {
		GET_TRANSFORMS, PERSIST_TRANSFORMS, PERSIST_TRANSFORMS_SUCCESS,
		PERSIST_TRANSFORMS_FAILURE, FINISHED
	}

	String dbPrefix;

	public List<DTRSimpleSerialWrapper> transformsToPersistOnServer;

	public ModalNotifier notifier;

	public Throwable remotePersistenceException;

	private AsyncCallback completionCallback;

	@Override
	public void onFailure(Throwable throwable) {
		completionCallback.onFailure(throwable);
	}

	class Player_GET_TRANSFORMS
			extends
			EnumRunnableAsyncCallbackPlayer<List<DTRSimpleSerialWrapper>, State> {
		public Player_GET_TRANSFORMS() {
			super(State.GET_TRANSFORMS);
		}

		@Override
		public void run() {
			LocalTransformPersistence.get().getTransforms(
					DomainTransformRequestType.TO_REMOTE, this);
		}

		@Override
		public void onSuccess(List<DTRSimpleSerialWrapper> result) {
			transformsToPersistOnServer = result;
			if (result.isEmpty()) {
				consort.wasPlayed(this,
						Collections.singletonList(State.FINISHED));
			} else {
				super.onSuccess(result);
			}
		}
	}

	class Player_PERSIST_TRANSFORMS extends
			EnumRunnableAsyncCallbackPlayer<Void, State> {
		public Player_PERSIST_TRANSFORMS() {
			super(State.PERSIST_TRANSFORMS);
		}

		@Override
		public Collection<State> getProvides() {
			return Arrays.asList(new State[] {
					State.PERSIST_TRANSFORMS_SUCCESS,
					State.PERSIST_TRANSFORMS_FAILURE });
		}

		@Override
		public void run() {
			LocalTransformPersistence.get()
					.getCommitToStorageTransformListener().setPaused(true);
			String message = TextProvider.get().getUiObjectText(
					LocalTransformPersistence.class, "saving-unsaved-message",
					"Saving unsaved work from previous session");
			notifier = ClientLayerLocator.get().notifications()
					.getModalNotifier(message);
			notifier.setMasking(false);
			LocalTransformPersistence.get().persistOfflineTransforms(
					transformsToPersistOnServer, notifier, this);
		}

		public void onFailure(Throwable caught) {
			cleanup();
			if (ClientUtils.maybeOffline(caught)) {
				consort.onFailure(caught);
				return;
			}
			remotePersistenceException = caught;
			consort.wasPlayed(this,
					Collections.singletonList(State.PERSIST_TRANSFORMS_FAILURE));
		}

		public void onSuccess(Void result) {
			cleanup();
			consort.wasPlayed(this,
					Collections.singletonList(State.PERSIST_TRANSFORMS_SUCCESS));
		}

		private void cleanup() {
			LocalTransformPersistence.get()
					.getCommitToStorageTransformListener().setPaused(false);
			notifier.modalOff();
		}
	}

	class Player_PERSIST_TRANSFORMS_FAILURE extends
			EnumRunnableAsyncCallbackPlayer<Void, State> {
		public Player_PERSIST_TRANSFORMS_FAILURE() {
			super(State.PERSIST_TRANSFORMS_FAILURE, State.FINISHED);
		}

		@Override
		public void run() {
			new FromOfflineConflictResolver().resolve(
					transformsToPersistOnServer, remotePersistenceException,
					LocalTransformPersistence.get(), this);
		}
	}

	class Player_PERSIST_TRANSFORMS_SUCCCESS extends
			EnumRunnableAsyncCallbackPlayer<Void, State> {
		public Player_PERSIST_TRANSFORMS_SUCCCESS() {
			super(State.PERSIST_TRANSFORMS_SUCCESS, State.FINISHED);
		}

		@Override
		public void run() {
			LocalTransformPersistence.get().transformPersisted(
					transformsToPersistOnServer, this);
		}

		@Override
		public void onSuccess(Void result) {
			ClientLayerLocator.get().notifications()
					.notifyOfCompletedSaveFromOffline();
			super.onSuccess(result);
		}
	}

	public PersistOfflineTransformsConsort(AsyncCallback completionCallback) {
		this.completionCallback = completionCallback;
		addPlayer(new Player_GET_TRANSFORMS());
		addPlayer(new Player_PERSIST_TRANSFORMS());
		addPlayer(new Player_PERSIST_TRANSFORMS_FAILURE());
		addPlayer(new Player_PERSIST_TRANSFORMS_SUCCCESS());
		addEndpointPlayer(completionCallback);
		nudge();
	}
}
