package cc.alcina.framework.gwt.persistence.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.EnumPlayer.EnumRunnableAsyncCallbackPlayer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.gwt.persistence.client.UploadOfflineTransformsConsort.State;

public class UploadOfflineTransformsConsort extends Consort<State> {
	public static final String TOPIC_PERSIST_TRANSFORMS_FAILURE = UploadOfflineTransformsConsort.class
			.getName() + ".PERSIST_TRANSFORMS_FAILURE";

	public static void notifyPersistTransformsFailure(Throwable ex) {
		GlobalTopicPublisher.get()
				.publishTopic(TOPIC_PERSIST_TRANSFORMS_FAILURE, ex);
	}

	public static void notifyPersistTransformsFailureListenerDelta(
			TopicListener<Throwable> listener, boolean add) {
		GlobalTopicPublisher.get()
				.listenerDelta(TOPIC_PERSIST_TRANSFORMS_FAILURE, listener, add);
	}

	String dbPrefix;

	public List<DeltaApplicationRecord> transformsToPersistOnServer;

	public Throwable remotePersistenceException;

	private AsyncCallback completionCallback;

	public UploadOfflineTransformsConsort(AsyncCallback completionCallback) {
		this.completionCallback = completionCallback;
		addPlayer(new Player_CHECK_OFFLINE());
		addPlayer(new Player_GET_TRANSFORMS());
		addPlayer(new Player_PERSIST_TRANSFORMS());
		addPlayer(new Player_PERSIST_TRANSFORMS_FAILURE());
		addPlayer(new Player_PERSIST_TRANSFORMS_SUCCCESS());
		addEndpointPlayer(completionCallback, true);
		nudge();
	}

	public boolean isPersistenceFailed() {
		return containsState(State.PERSIST_TRANSFORMS_FAILURE);
	}

	@Override
	public void onFailure(Throwable throwable) {
		completionCallback.onFailure(throwable);
	}

	class Player_CHECK_OFFLINE
			extends EnumRunnableAsyncCallbackPlayer<Void, State> {
		public Player_CHECK_OFFLINE() {
			super(State.CHECK_OFFLINE);
		}

		@Override
		public void onFailure(Throwable caught) {
			if (ClientUtils.maybeOffline(caught)) {
				consort.onFailure(caught);
				return;
			}
			remotePersistenceException = caught;
			wasPlayed(State.PERSIST_TRANSFORMS_FAILURE);
		}

		@Override
		public void run() {
			ClientBase.getCommonRemoteServiceAsyncInstance().ping(this);
		}
	}

	class Player_GET_TRANSFORMS extends
			EnumRunnableAsyncCallbackPlayer<Iterator<DeltaApplicationRecord>, State> {
		public Player_GET_TRANSFORMS() {
			super(State.GET_TRANSFORMS);
		}

		@Override
		public void onSuccess(Iterator<DeltaApplicationRecord> result) {
			transformsToPersistOnServer = CommonUtils.iteratorToList(result);
			if (transformsToPersistOnServer.isEmpty()) {
				wasPlayed(State.FINISHED);
			} else {
				super.onSuccess(result);
			}
		}

		@Override
		public void run() {
			LocalTransformPersistence.get().getTransforms(
					DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED, this);
		}
	}

	class Player_PERSIST_TRANSFORMS
			extends EnumRunnableAsyncCallbackPlayer<Void, State> {
		public Player_PERSIST_TRANSFORMS() {
			super(State.PERSIST_TRANSFORMS);
		}

		@Override
		public Collection<State> getProvides() {
			return Arrays.asList(new State[] { State.PERSIST_TRANSFORMS_SUCCESS,
					State.PERSIST_TRANSFORMS_FAILURE });
		}

		public void onFailure(Throwable caught) {
			cleanup();
			if (ClientUtils.maybeOffline(caught)) {
				consort.onFailure(caught);
				return;
			}
			remotePersistenceException = caught;
			wasPlayed(State.PERSIST_TRANSFORMS_FAILURE);
		}

		public void onSuccess(Void result) {
			cleanup();
			wasPlayed(State.PERSIST_TRANSFORMS_SUCCESS);
		}

		@Override
		public void run() {
			LocalTransformPersistence.get()
					.getCommitToStorageTransformListener().setPaused(true);
			String message = TextProvider.get().getUiObjectText(
					LocalTransformPersistence.class, "saving-unsaved-message",
					"Saving unsaved work from previous session");
			ModalNotifier notifier = Registry.impl(HandshakeConsortModel.class)
					.ensureLoadObjectsNotifier(message);
			notifier.modalOff();
			LocalTransformPersistence.get().persistOfflineTransforms(
					transformsToPersistOnServer, notifier, this);
		}

		private void cleanup() {
			LocalTransformPersistence.get()
					.getCommitToStorageTransformListener().setPaused(false);
		}
	}

	class Player_PERSIST_TRANSFORMS_FAILURE
			extends EnumRunnableAsyncCallbackPlayer<Void, State> {
		public Player_PERSIST_TRANSFORMS_FAILURE() {
			super(State.PERSIST_TRANSFORMS_FAILURE, State.FINISHED);
		}

		@Override
		public void run() {
			notifyPersistTransformsFailure(remotePersistenceException);
			Registry.impl(FromOfflineConflictResolver.class).resolve(
					transformsToPersistOnServer, remotePersistenceException,
					LocalTransformPersistence.get(), this);
		}
	}

	class Player_PERSIST_TRANSFORMS_SUCCCESS
			extends EnumRunnableAsyncCallbackPlayer<Void, State> {
		public Player_PERSIST_TRANSFORMS_SUCCCESS() {
			super(State.PERSIST_TRANSFORMS_SUCCESS, State.FINISHED);
		}

		@Override
		public void onSuccess(Void result) {
			Registry.impl(ClientNotifications.class)
					.notifyOfCompletedSaveFromOffline();
			super.onSuccess(result);
		}

		@Override
		public void run() {
			LocalTransformPersistence.get()
					.transformPersisted(transformsToPersistOnServer, this);
		}
	}

	static enum State {
		CHECK_OFFLINE, GET_TRANSFORMS, PERSIST_TRANSFORMS,
		PERSIST_TRANSFORMS_SUCCESS, PERSIST_TRANSFORMS_FAILURE, FINISHED
	}
}
