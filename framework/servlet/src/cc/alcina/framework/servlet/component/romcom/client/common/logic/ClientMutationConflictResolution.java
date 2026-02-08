package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.gwt.client.dirndl.model.edit.Feature_Dirndl_MutationConflictResolution;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AfterHandled;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.BeforeHandled;

@Feature.Ref(Feature_Dirndl_MutationConflictResolution.class)
class ClientMutationConflictResolution {
	/**
	 * Reject server mutation messages if the client has modified in a timeline
	 * not visible to the server at mutation time
	 */
	class CheckForRejection implements
			ProcessObserver<RemoteComponentProtocol.Message.BeforeHandled> {
		@Override
		public void topicPublished(BeforeHandled message) {
		}
	}

	/**
	 * Register client elements for possible mutation rejection
	 */
	class RejectMutationsBefore implements
			ProcessObserver<RemoteComponentProtocol.Message.AfterHandled> {
		@Override
		public void topicPublished(AfterHandled message) {
		}
	}

	ClientMutationConflictResolution() {
		new CheckForRejection().bind();
		new RejectMutationsBefore().bind();
	}
}
