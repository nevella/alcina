package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.gwt.client.dirndl.model.edit.EditAreaBehavior.RejectConflictingMutation;
import cc.alcina.framework.gwt.client.dirndl.model.edit.Feature_Dirndl_MutationConflictResolution;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageId;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.SendChannelId;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.BeforeHandled;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.OnQueued;

/*
 * Note that much of this code is shared, since the server environment behaves
 * 'like the client' - the client just rejects, the server needs to reject AND
 * merge.
 * 
 * Tracking on the server as well as the client avoids waiting for in-flight
 * messages to cross
 */
@Feature.Ref(Feature_Dirndl_MutationConflictResolution.class)
public abstract class MutationConflictResolution {
	/**
	 * Reject server mutation messages if the client has modified in a timeline
	 * not visible to the server at mutation time
	 * 
	 * This code works both client + server - the client rejects the server
	 * mutation, wheras the server rejects (initially) ths client mutation
	 */
	protected class TestRejectionObserver implements
			ProcessObserver<RemoteComponentProtocol.Message.BeforeHandled> {
		public TestRejectionObserver() {
		}

		@Override
		public void topicPublished(BeforeHandled observable) {
			RemoteComponentProtocol.Message message = observable.message;
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
						}
					}
				});
				if (conflictingMutations.hasConflicts()) {
					onHasConflicts(observable, conflictingMutations);
				}
			}
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
			implements ProcessObserver<Mutations.Rejected> {
		public PerformPartialUndo() {
		}

		@Override
		public void topicPublished(Mutations.Rejected observable) {
			int debug = 3;
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
			this.mutationsList.add(mutations);
			newestLocalMutationId = mutations.messageId;
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

	Map<Element, EditCurrency> elementCurrency = AlcinaCollections
			.newLinkedHashMap();

	protected abstract void onHasConflicts(BeforeHandled observable,
			ConflictingMutations conflictingMutations);

	void maybeUpdateRejectionCurrency(RemoteComponentProtocol.Message message) {
		if (message instanceof Mutations) {
			Mutations mutations = (Mutations) message;
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
