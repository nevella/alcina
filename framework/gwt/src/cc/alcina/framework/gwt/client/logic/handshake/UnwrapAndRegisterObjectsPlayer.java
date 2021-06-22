package cc.alcina.framework.gwt.client.logic.handshake;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.SerializationException;

import cc.alcina.framework.common.client.collections.IteratorWithCurrent;
import cc.alcina.framework.common.client.logic.RepeatingCommandWithPostCompletionCallback;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelObjectsRegistrar;
import cc.alcina.framework.common.client.logic.domaintransform.HasRequestReplayId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.LoopingPlayer;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasSize;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.entity.GeneralProperties;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.persistence.client.DteReplayWorker;
import cc.alcina.framework.gwt.persistence.client.DtrWrapperBackedDomainModelDelta;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
public class UnwrapAndRegisterObjectsPlayer
		extends RunnableAsyncCallbackPlayer<Void, HandshakeState>
		implements LoopingPlayer {
	public static final String TOPIC_DELTA_PROGRESS = UnwrapAndRegisterObjectsPlayer.class
			.getName() + ".TOPIC_DELTA_PROGRESS";

	public static void deltaProgress(IntPair intPair) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_DELTA_PROGRESS, intPair);
	}

	public static void deltaProgressListenerDelta(
			TopicListener<IntPair> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(TOPIC_DELTA_PROGRESS, listener,
				add);
	}

	protected DomainModelDelta currentDelta = null;

	private Phase phase;

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
		return Ax.format(
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

	@Override
	public void onFailure(Throwable caught) {
		caught.printStackTrace();
		if (CommonUtils.hasCauseOfClass(caught, SerializationException.class)) {
			consort.wasPlayed(this, Collections.singletonList(
					HandshakeState.OBJECTS_FATAL_DESERIALIZATION_EXCEPTION));
		} else {
			// code failure in post-ok handler
			consort.onFailure(caught);
		}
	}

	@Override
	public void onSuccess(Void result) {
		consort.replay(this);
	}

	@Override
	public void run() {
		if (HandshakeConsortModel.get().getLoadObjectsResponse() == null
				&& isBypassInitialObjectsRegistration()) {
			consort.wasPlayed(this, Collections.singletonList(
					HandshakeState.OBJECTS_UNWRAPPED_AND_REGISTERED));
			return;
		}
		HandshakeConsortModel.get().prepareInitialPlaySequence();
		loop();
	}

	protected boolean isBypassInitialObjectsRegistration() {
		return false;
	}

	private void loop0() {
		if (phase == null || phase == Phase.REPLAYING_TRANSFORMS) {
			currentDelta = null;
		} else {
			phase = Phase.values()[phase.ordinal() + 1];
		}
		if (currentDelta == null) {
			IteratorWithCurrent<DomainModelDelta> deltasToApply = HandshakeConsortModel
					.get().getDeltasToApply();
			currentDelta = deltasToApply.current();
			deltasToApply.moveNext();
			if (currentDelta != null) {
				deltaOrdinal++;
				Iterator<DomainModelDelta> itr = deltasToApply.getItr();
				if (itr instanceof HasSize) {
					deltaProgress(new IntPair(deltaOrdinal,
							((HasSize) itr).getSize()));
				}
				phase = Phase.UNWRAPPING;
			} else {
				HandshakeConsortModel.get().ensureLoadObjectsNotifier("")
						.modalOff();
				consort.wasPlayed(this, Collections.singletonList(
						HandshakeState.OBJECTS_UNWRAPPED_AND_REGISTERED));
				return;
			}
		}
		// handshakeConsortModel.ensureLoadObjectsNotifier(
		// Ax.format("Register: %s - %s", deltaOrdinal,
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
			if (CommonUtils
					.isNotNullOrEmpty(currentDelta.getUnlinkedObjects())) {
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

	private boolean maybeRegisterDomainModelObjects() {
		if (currentDelta.getDomainModelObject() != null) {
			Registry.impl(DomainModelObjectsRegistrar.class)
					.registerAsync(currentDelta.getDomainModelObject(), this);
			return true;
		}
		return false;
	}

	private void registerUnlinked() {
		TransformManager.get().registerDomainObjectsAsync(
				(Collection) currentDelta.getUnlinkedObjects(), this);
	}

	protected boolean maybeRegisterDomainModelHolder() {
		// we can expect the first delta to have a domainmodelholder -
		// apps which allow "always offline" should create a model holder if the
		// first delta doesn't have a holder;
		if (currentDelta.getDomainModelHolder() != null) {
			if (currentDelta instanceof DtrWrapperBackedDomainModelDelta) {
				DeltaApplicationRecordType type = ((DtrWrapperBackedDomainModelDelta) currentDelta)
						.getWrapper().getType();
				if (type == DeltaApplicationRecordType.REMOTE_DELTA_APPLIED) {
					HandshakeConsortModel.get().setPriorRemoteConnections(true);
				}
			}
			registerDomainModelHolder(currentDelta.getDomainModelHolder());
			return true;
		}
		return false;
	}

	protected void
			registerDomainModelHolder(DomainModelHolder domainModelHolder) {
		domainModelHolder.registerSelfAsProvider();
		GeneralProperties generalProperties = domainModelHolder
				.getGeneralProperties();
		HandshakeConsortModel.get().registerInitialObjects(
				domainModelHolder.getGeneralProperties(),
				domainModelHolder.getCurrentUser());
		TransformManager.get()
				.registerDomainObjectsInHolderAsync(domainModelHolder, this);
	}

	protected void replayTransforms() {
		replayer = new RepeatingCommandWithPostCompletionCallback(this,
				new DteReplayWorker(currentDelta.getReplayEvents()));
		if (currentDelta.hasLocalOnlyTransforms()) {
			HandshakeConsortModel.get().setLoadedWithLocalOnlyTransforms(true);
		}
		Integer requestId = (currentDelta instanceof HasRequestReplayId)
				? ((HasRequestReplayId) currentDelta)
						.getDomainTransformRequestReplayId()
				: null;
		if (requestId != null) {
			CommitToStorageTransformListener tl = Registry
					.impl(CommitToStorageTransformListener.class);
			tl.setLocalRequestId(
					Math.max(requestId + 1, (int) tl.getLocalRequestId()));
		}
		Scheduler.get().scheduleIncremental(replayer);
	}

	enum Phase {
		UNWRAPPING, REGISTERING_GRAPH, REGISTERING_UNLINKED,
		REPLAYING_TRANSFORMS
	}
}