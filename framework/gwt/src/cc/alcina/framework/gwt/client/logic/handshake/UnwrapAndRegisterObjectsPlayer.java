package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.RepeatingCommandWithPostCompletionCallback;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.HasRequestReplayId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.LoopingPlayer;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd.ReloadOnSuccessCallback;
import cc.alcina.framework.gwt.persistence.client.DteReplayWorker;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Window;

/**
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class UnwrapAndRegisterObjectsPlayer extends
		RunnableAsyncCallbackPlayer<Void, HandshakeState> implements
		LoopingPlayer {
	public UnwrapAndRegisterObjectsPlayer() {
		addRequires(HandshakeState.OBJECT_DATA_LOADED);
		addProvides(HandshakeState.OBJECTS_UNWRAPPED_AND_REGISTERED);
	}

	@Override
	public void onSuccess(Void result) {
		consort.replay(this);
	}

	@Override
	public void onFailure(Throwable caught) {
		if (Window.confirm("Failure in unwrap/register -  press 'oi' to clear")) {
			LocalTransformPersistence.get().clearPersistedClient(null, -1,
					new ReloadOnSuccessCallback(),true);
			return;
		}
		super.onFailure(caught);
	}

	@Override
	public String describeLoop() {
		return CommonUtils.formatJ(
				"Chews through deltas in the handshakeConsortModel"
						+ " - for each in sequence [%s] - see javadoc ",
				CommonUtils.join(Phase.values(), ", "));
	}

	DomainModelDelta currentDelta = null;

	enum Phase {
		UNWRAPPING, REGISTERING_GRAPH, REGISTERING_UNLINKED,
		REPLAYING_TRANSFORMS
	}

	private Phase phase;

	@SuppressWarnings("unused")
	private int deltaOrdinal = 0;

	private RepeatingCommandWithPostCompletionCallback replayer;

	@Override
	public void run() {
		HandshakeConsortModel.get().prepareInitialPlaySequence();
		loop();
	}

	@Override
	public void loop() {
		if (phase == null || phase == Phase.REPLAYING_TRANSFORMS) {
			currentDelta = null;
		} else {
			phase = Phase.values()[phase.ordinal() + 1];
		}
		if (currentDelta == null) {
			currentDelta = HandshakeConsortModel.get().getDeltasToApply()
					.current();
			HandshakeConsortModel.get().getDeltasToApply().moveNext();
			if (currentDelta != null) {
				deltaOrdinal++;
				phase = Phase.UNWRAPPING;
			} else {
				HandshakeConsortModel.get().ensureLoadObjectsNotifier("")
						.modalOff();
				wasPlayed();
				return;
			}
		}
		// handshakeConsortModel.ensureLoadObjectsNotifier(
		// CommonUtils.formatJ("Register: %s - %s", deltaOrdinal,
		// CommonUtils.friendlyConstant(phase).toLowerCase()))
		// .modalOn();
		switch (phase) {
		case UNWRAPPING:
			currentDelta.unwrap(this);
			return;
		case REGISTERING_GRAPH:
			if (currentDelta.getDomainModelHolder() != null) {
				registerDomainModelHolder();
				return;
			}
			break;
		case REGISTERING_UNLINKED:
			if (CommonUtils.isNotNullOrEmpty(currentDelta.getUnlinkedObjects())) {
				registerUnlinked();
				return;
			}
			break;
		case REPLAYING_TRANSFORMS:
			if (CommonUtils.isNotNullOrEmpty(currentDelta.getReplayEvents())) {
				replayTransforms();
				return;
			}
			break;
		}
		consort.replay(this);
	}

	@Override
	public void cancel() {
		if (replayer != null) {
			replayer.setCancelled(true);
		}
		super.cancel();
	}

	private void registerUnlinked() {
		TransformManager.get().registerDomainObjectsAsync(
				currentDelta.getUnlinkedObjects(), this);
	}

	private void replayTransforms() {
		replayer = new RepeatingCommandWithPostCompletionCallback(this,
				new DteReplayWorker(currentDelta.getReplayEvents()));
		Integer requestId = (currentDelta instanceof HasRequestReplayId) ? ((HasRequestReplayId) currentDelta)
				.getDomainTransformRequestReplayId() : null;
		if (requestId != null) {
			CommitToStorageTransformListener tl = ClientLayerLocator.get()
					.getCommitToStorageTransformListener();
			tl.setLocalRequestId(Math.max(requestId + 1,
					(int) tl.getLocalRequestId()));
		}
		Scheduler.get().scheduleIncremental(replayer);
	}

	protected void registerDomainModelHolder() {
		DomainModelHolder domainObjects = currentDelta.getDomainModelHolder();
		domainObjects.registerSelfAsProvider();
		GeneralProperties generalProperties = domainObjects
				.getGeneralProperties();
		if (generalProperties != null) {
			Registry.putSingleton(GeneralProperties.class, generalProperties);
		}
		PermissionsManager.get().setUser(domainObjects.getCurrentUser());
		ClientLayerLocator.get().setDomainModelHolder(domainObjects);
		PermissionsManager.get().setLoginState(
				HandshakeConsortModel.get().getLoginState());
		ClientLayerLocator
				.get()
				.notifications()
				.log(CommonUtils.formatJ("User: %s", domainObjects
						.getCurrentUser() == null ? null : domainObjects
						.getCurrentUser().getUserName()));
		TransformManager.get().registerDomainObjectsInHolderAsync(
				domainObjects, this);
	}
}