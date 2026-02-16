package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.gwt.client.dirndl.model.edit.EditAreaBehavior.RejectConflictingMutation;
import cc.alcina.framework.gwt.client.dirndl.model.edit.Feature_Dirndl_MutationConflictResolution;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageId;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.SendChannelId;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.BeforeHandled;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.HasTimeline;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.OnQueued;

/*
 * Note that much of this code is shared, since the server environment behaves
 * 'like the client' - the client just rejects, the server needs to reject AND
 * merge.
 * 
 * Tracking on the server as well as the client avoids waiting for in-flight
 * messages to cross
 * 
 * @formatter:off

Invariants/points to consider

* client mutations which affect descendants of a server-removed CE tree are discarded [todo: test]
* they also won't trigger the rejection rules (verify)
* server mutations which should be rewritten are precisely those which have not been applied to the client (see computeRejectedMessages)
* because edits form trees, we can build the resolved mutation sequence by 
  (re)-sequencing *all* server-side edits, then applying undos - rather than interleaving
* the assumption is that the ce handling is totally 'reactive' (i.e. a fragmentmodel)





 * @formatter:on
 */
@Feature.Ref(Feature_Dirndl_MutationConflictResolution.class)
public abstract class MutationConflictResolution {
	public static class Rejected implements ContextObservable {
		public Mutations mutations;

		public ConflictingMutations conflictingMutations;

		public Rejected(Mutations mutations,
				ConflictingMutations conflictingMutations) {
			this.mutations = mutations;
			this.conflictingMutations = conflictingMutations;
		}
	}

	/**
	 * Reject server mutation messages if the client has modified in a timeline
	 * not visible to the server at mutation time
	 * 
	 * This code works both client + server - if there's conflict, the client
	 * rejects the server mutation, wheras the server effectively prepends
	 * conflicting mutation undos to ths client mutation
	 */
	@Reflected
	protected class TestRejectionObserver implements
			ProcessObserver<RemoteComponentProtocol.Message.BeforeHandled> {
		public TestRejectionObserver() {
		}

		@Override
		public void topicPublished(BeforeHandled observable) {
			RemoteComponentProtocol.Message message = observable.message;
			if (message instanceof HasTimeline) {
				if (isFromOtherEndpoit(message)) {
					MessageId counterpartProcessingId = ((HasTimeline) message)
							.getCounterpartProcessingId();
					onSeenOnCounterpart(counterpartProcessingId);
				}
			}
			if (message instanceof Mutations) {
				ConflictingMutations conflictingMutations = new ConflictingMutations();
				Mutations mutations = (Mutations) message;
				mutations.domMutations.forEach(record -> {
					Node affectedNode = record.target.attachId.node();
					/*
					 * to-be-attached nodes will be null here, but they can be
					 * ignored (it's the parent mod that counts here)
					 */
					if (affectedNode != null) {
						Element rejectBehavior = RejectConflictingMutation
								.ancestorWithBehavior(affectedNode);
						if (rejectBehavior != null
								&& testReject(rejectBehavior, mutations)) {
							conflictingMutations.conflicting.add(record);
							conflictingMutations.withConflctBehaviors
									.add(rejectBehavior);
						}
					}
				});
				if (conflictingMutations.hasConflicts()) {
					onHasConflicts(observable, conflictingMutations);
				}
			}
		}

		private boolean
				isFromOtherEndpoit(RemoteComponentProtocol.Message message) {
			return message.messageId.sendChannelId == MessageTransportLayer
					.get().sendChannelId().oppositeEndpointId();
		}
	}

	/**
	 * Register client elements for possible mutation rejection. Client only
	 */
	protected class RejectHappensBeforeQueuedMutations implements
			ProcessObserver<RemoteComponentProtocol.Message.OnQueued> {
		public RejectHappensBeforeQueuedMutations() {
		}

		@Override
		public void topicPublished(OnQueued observable) {
			if (observable.sendChannelId == SendChannelId.SERVER_TO_CLIENT) {
				return;
			}
			RemoteComponentProtocol.Message message = observable.message;
			maybeUpdateRejectionCurrency(message);
		}
	}

	/**
	 * Register server elements for possible mutation rejection. Server only
	 */
	protected class RejectHappensAfterQueuedMutations implements
			ProcessObserver<RemoteComponentProtocol.Message.OnQueued> {
		public RejectHappensAfterQueuedMutations() {
		}

		@Override
		public void topicPublished(OnQueued observable) {
			if (observable.sendChannelId == SendChannelId.CLIENT_TO_SERVER) {
				return;
			}
			RemoteComponentProtocol.Message message = observable.message;
			maybeUpdateRejectionCurrency(message);
		}
	}

	protected static class ConflictingMutations {
		Set<Element> withConflctBehaviors = new LinkedHashSet<>();

		List<MutationRecord> conflicting = new ArrayList<>();

		boolean hasConflicts() {
			return conflicting.size() > 0;
		}
	}

	/**
	 * Perform the undo.
	 */
	/*
	 * Steps:
	 * 
	 * - populate the pending to-client mutationrecord with the mutations of the
	 * rejected record
	 * 
	 * - generate + apply undo (inverse) mutations for conflicting mutations (in
	 * reverse order)
	 * 
	 * - flush listeners
	 */
	protected class PerformPartialUndo
			implements ProcessObserver<MutationConflictResolution.Rejected> {
		List<Mutations> rejectedMessages;

		List<MutationRecord> rejectedMutations;

		Rejected observable;

		public PerformPartialUndo() {
		}

		@Override
		public void
				topicPublished(MutationConflictResolution.Rejected observable) {
			this.observable = observable;
			computeRejectedMessages();
			computeRejectedMutations();
			populateCurrentMutationsMessage();
			generateAndApplyUndoMutations();
			// flush required?
		}

		void computeRejectedMutations() {
			/*
			 * incoming conflicting mutations give us the behaviors which
			 * indicate rejection - but they're client_server - we need to
			 * reject the corresponding server_client mutations
			 * 
			 */
			Set<Element> withConflctBehaviors = observable.conflictingMutations.withConflctBehaviors;
			rejectedMutations = rejectedMessages.stream()
					.flatMap(m -> m.domMutations.stream())
					.filter(m -> withConflctBehaviors.stream().anyMatch(wcb -> {
						Node node = m.target.node();
						if (node == null) {
							/*
							 * was removed
							 */
							throw new UnsupportedOperationException();
						}
						return wcb.provideIsAncestorOf(node, true);
					})).toList();
		}

		void generateAndApplyUndoMutations() {
			List<MutationRecord> reversed = rejectedMutations.stream()
					.collect(Collectors.toList());
			Collections.reverse(reversed);
			/*
			 * note that this doesn't currently handle eventsystemmutation undo
			 * (or behavior) - but given we're most likely *undoing* behavior
			 * add, we can limp by...
			 */
			List<MutationRecord> inversions = reversed.stream()
					.map(MutationRecord::invert).toList();
			LocalDom.attachIdRepresentations().applyMutations(inversions,
					false);
		}

		void populateCurrentMutationsMessage() {
			Set<MutationRecord> rejectedMutationSet = rejectedMutations.stream()
					.collect(AlcinaCollectors.toLinkedHashSet());
			Set<Node> rejectedMutationNodes = rejectedMutationSet.stream()
					.map(m -> m.target.node).collect(Collectors.toSet());
			rejectedMessages.forEach(mutations -> {
				Mutations resubmit = new Mutations();
				mutations.domMutations.stream()
						.filter(m -> !rejectedMutationSet.contains(m))
						.forEach(resubmit.domMutations::add);
				mutations.eventSystemMutations.stream()
						.filter(esm -> !rejectedMutationNodes
								.contains(esm.nodeId.node()))
						.forEach(resubmit.eventSystemMutations::add);
				resubmit.locationMutation = mutations.locationMutation;
				/*
				 * do not resubmit the mutations.selectionMutation
				 */
				new Mutations.Resubmit(resubmit).publish();
			});
		}

		void computeRejectedMessages() {
			rejectedMessages = uncommittedOnCounterpart.stream().toList();
		}
	}

	class EditCurrency {
		Element withRejectBehavior;

		MessageId newestLocalMutationId;

		/**
		 * these are retained for undo, and removed if testReject passes
		 */
		List<Mutations> mutationsList = new ArrayList<>();

		EditCurrency(Element withRejectBehavior) {
			this.withRejectBehavior = withRejectBehavior;
		}

		void addLocalMutations(Mutations mutations) {
			if (!mutationsList.contains(mutations)) {
				mutationsList.add(mutations);
				newestLocalMutationId = mutations.messageId;
			}
		}

		boolean testReject(Mutations mutations) {
			boolean reject = newestLocalMutationId != null
					&& newestLocalMutationId
							.compareTo(mutations.counterpartProcessingId) > 0;
			if (!reject) {
				mutationsList.removeIf(m -> m.messageId
						.compareTo(mutations.counterpartProcessingId) <= 0);
			}
			return reject;
		}
	}

	List<Mutations> uncommittedOnCounterpart = new ArrayList<>();

	Map<Element, EditCurrency> elementCurrency = AlcinaCollections
			.newLinkedHashMap();

	protected abstract void onHasConflicts(BeforeHandled observable,
			ConflictingMutations conflictingMutations);

	void onSeenOnCounterpart(MessageId counterpartProcessingId) {
		uncommittedOnCounterpart.removeIf(m -> {
			boolean remove = m.messageId
					.compareTo(counterpartProcessingId) <= 0;
			return remove;
		});
	}

	void maybeUpdateRejectionCurrency(RemoteComponentProtocol.Message message) {
		if (message instanceof Mutations) {
			Mutations mutations = (Mutations) message;
			uncommittedOnCounterpart.add(mutations);
			mutations.domMutations.forEach(record -> {
				Node affectedNode = record.target.attachId.node();
				if (affectedNode != null) {
					Element rejectBehavior = RejectConflictingMutation
							.ancestorWithBehavior(affectedNode);
					if (rejectBehavior != null) {
						ensureCurrency(rejectBehavior)
								.addLocalMutations(mutations);
					}
				}
			});
		}
	}

	boolean testReject(Element withRejectBehavior, Mutations mutations) {
		return ensureCurrency(withRejectBehavior).testReject(mutations);
	}

	EditCurrency ensureCurrency(Element withRejectBehavior) {
		return elementCurrency.computeIfAbsent(withRejectBehavior,
				EditCurrency::new);
	}
}
