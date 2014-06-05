package cc.alcina.framework.gwt.client.logic.handshake;

import java.util.Collections;

import cc.alcina.framework.common.client.logic.RepeatingCommandWithPostCompletionCallback;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelObject;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelObjectsRegistrar;
import cc.alcina.framework.common.client.logic.domaintransform.HasRequestReplayId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.LoopingPlayer;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.persistence.client.DteReplayWorker;

import com.google.gwt.core.client.Scheduler;

/**
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class UnwrapAndRegisterObjectsPlayer extends
		RunnableAsyncCallbackPlayer<Void, HandshakeState> implements
		LoopingPlayer {
	protected DomainModelDelta currentDelta = null;

	private Phase phase;

	@SuppressWarnings("unused")
	private int deltaOrdinal = 0;

	protected RepeatingCommandWithPostCompletionCallback replayer;

	public UnwrapAndRegisterObjectsPlayer() {
		addRequires(HandshakeState.OBJECT_DATA_LOADED);
		addProvides(HandshakeState.OBJECTS_UNWRAPPED_AND_REGISTERED);
		addProvides(HandshakeState.OBJECTS_FATAL_DESERIALIZATION_EXCEPTION);
	}

	@Override
	public void cancel() {
		if (replayer != null) {
			replayer.setCancelled(true);
		}
		super.cancel();
	}

	@Override
	public String describeLoop() {
		return CommonUtils.formatJ(
				"Chews through deltas in the handshakeConsortModel"
						+ " - for each in sequence [%s] - see javadoc ",
				CommonUtils.join(Phase.values(), ", "));
	}

	@Override
	public void loop() {
		try {
			loop0();
		} catch (Exception e) {
			onFailure(e);
		}
	}

	private void loop0() {
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
				consort.wasPlayed(
						this,
						Collections
								.singletonList(HandshakeState.OBJECTS_UNWRAPPED_AND_REGISTERED));
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
			if (maybeRegisterDomainModelHolder()) {
				return;
			}
			if (maybeRegisterDomainModelObjects()) {
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
	public void onFailure(Throwable caught) {
		caught.printStackTrace();
		if (consort.containsState(HandshakeState.OBJECT_DATA_LOADED)) {
			// code failure in post-ok handler
			consort.onFailure(caught);
		} else {
			consort.wasPlayed(
					this,
					Collections
							.singletonList(HandshakeState.OBJECTS_FATAL_DESERIALIZATION_EXCEPTION));
		}
	}

	@Override
	public void onSuccess(Void result) {
		consort.replay(this);
	}

	@Override
	public void run() {
		HandshakeConsortModel.get().prepareInitialPlaySequence();
		loop();
	}

	private boolean maybeRegisterDomainModelObjects() {
		if (currentDelta.getDomainModelObject() != null) {
			Registry.impl(DomainModelObjectsRegistrar.class).registerAsync(
					currentDelta.getDomainModelObject(), this);
			return true;
		}
		return false;
	}

	private void registerUnlinked() {
		TransformManager.get().registerDomainObjectsAsync(
				currentDelta.getUnlinkedObjects(), this);
	}

	protected boolean maybeRegisterDomainModelHolder() {
		// we can expect the first delta to be have a domainmodelholder -
		// apps which allow "always offline" should create a model holder if the
		// first delta doesn't have a holder;
		if (currentDelta.getDomainModelHolder() != null) {
			registerDomainModelHolder(currentDelta.getDomainModelHolder());
			return true;
		}
		return false;
	}

	protected void registerDomainModelHolder(DomainModelHolder domainModelHolder) {
		domainModelHolder.registerSelfAsProvider();
		GeneralProperties generalProperties = domainModelHolder
				.getGeneralProperties();
		HandshakeConsortModel.get().registerInitialObjects(
				domainModelHolder.getGeneralProperties(),
				domainModelHolder.getCurrentUser());
		TransformManager.get().registerDomainObjectsInHolderAsync(
				domainModelHolder, this);
	}

	protected void replayTransforms() {
		replayer = new RepeatingCommandWithPostCompletionCallback(this,
				new DteReplayWorker(currentDelta.getReplayEvents()));
		if (currentDelta.hasLocalOnlyTransforms()) {
			HandshakeConsortModel.get().setLoadedWithLocalOnlyTransforms(true);
		}
		Integer requestId = (currentDelta instanceof HasRequestReplayId) ? ((HasRequestReplayId) currentDelta)
				.getDomainTransformRequestReplayId() : null;
		if (requestId != null) {
			CommitToStorageTransformListener tl = Registry
					.impl(CommitToStorageTransformListener.class);
			tl.setLocalRequestId(Math.max(requestId + 1,
					(int) tl.getLocalRequestId()));
		}
		Scheduler.get().scheduleIncremental(replayer);
	}

	enum Phase {
		UNWRAPPING, REGISTERING_GRAPH, REGISTERING_UNLINKED,
		REPLAYING_TRANSFORMS
	}
}