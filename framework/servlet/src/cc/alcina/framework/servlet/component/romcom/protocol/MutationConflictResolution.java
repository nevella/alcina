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
import cc.alcina.framework.servlet.component.romcom.protocol.Mutations.MutationId;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.BeforeHandled;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.OnQueued;

@Feature.Ref(Feature_Dirndl_MutationConflictResolution.class)
public class MutationConflictResolution {
	/**
	 * Reject server mutation messages if the client has modified in a timeline
	 * not visible to the server at mutation time
	 */
	class TestRejectionObserver implements
			ProcessObserver<RemoteComponentProtocol.Message.BeforeHandled> {
		@Override
		public void topicPublished(BeforeHandled observable) {
			RemoteComponentProtocol.Message message = observable.message;
			if (message instanceof Mutations) {
				ConflictingMutations conflictingMutations = new ConflictingMutations();
				Mutations mutations = (Mutations) message;
				mutations.domMutations.forEach(record -> {
					Node affectedNode = record.target.node();
					Element rejectBehavior = RejectConflictingMutation
							.ancestorWithBehavior(affectedNode);
					if (rejectBehavior != null
							&& testReject(rejectBehavior, mutations)) {
						conflictingMutations.conflicting.add(record);
					}
				});
				if (conflictingMutations.hasConflicts()) {
					observable.cancelled = true;
					new Mutations.Rejected(mutations).publish();
				}
			}
		}
	}

	/**
	 * Register client elements for possible mutation rejection. Client only
	 */
	class RejectHappensBeforeMutations implements
			ProcessObserver<RemoteComponentProtocol.Message.OnQueued> {
		@Override
		public void topicPublished(OnQueued observable) {
			RemoteComponentProtocol.Message message = observable.message;
			if (message instanceof Mutations) {
				Mutations mutations = (Mutations) message;
				mutations.domMutations.forEach(record -> {
					Node affectedNode = record.target.node();
					Element rejectBehavior = RejectConflictingMutation
							.ancestorWithBehavior(affectedNode);
					ensureCurrency(
							rejectBehavior).newestLocalMutationId = mutations.mutationId;
				});
			}
		}
	}

	static class ConflictingMutations {
		List<MutationRecord> conflicting = new ArrayList<>();

		boolean hasConflicts() {
			return conflicting.size() > 0;
		}
	}

	class EditCurrency {
		Element withRejectBehavior;

		MutationId newestLocalMutationId;

		EditCurrency(Element withRejectBehavior) {
			this.withRejectBehavior = withRejectBehavior;
		}

		boolean testReject(Mutations mutations) {
			return newestLocalMutationId != null && newestLocalMutationId
					.compareTo(mutations.highestVisibleCounterpartId) > 0;
		}
	}

	boolean server;

	Map<Element, EditCurrency> elementCurrency = AlcinaCollections
			.newLinkedHashMap();

	public MutationConflictResolution(boolean server) {
		this.server = server;
		/*
		 * wip
		 */
		// new TestRejectionObserver().bind();
		// new RejectHappensBeforeMutations().bind();
	}

	boolean testReject(Element withRejectBehavior, Mutations mutations) {
		return ensureCurrency(withRejectBehavior).testReject(mutations);
	}

	EditCurrency ensureCurrency(Element withRejectBehavior) {
		return elementCurrency.computeIfAbsent(withRejectBehavior,
				EditCurrency::new);
	}
}
