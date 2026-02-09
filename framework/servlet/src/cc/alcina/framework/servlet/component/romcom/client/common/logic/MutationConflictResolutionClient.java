package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import cc.alcina.framework.servlet.component.romcom.protocol.MutationConflictResolution;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.BeforeHandled;

public class MutationConflictResolutionClient
		extends MutationConflictResolution {
	@Override
	protected void onHasConflicts(BeforeHandled observable,
			ConflictingMutations conflictingMutations) {
		observable.cancelled = true;
	}

	MutationConflictResolutionClient() {
		new TestRejectionObserver().bind();
		new RejectHappensBeforeQueuedMutations().bind();
	}
}
