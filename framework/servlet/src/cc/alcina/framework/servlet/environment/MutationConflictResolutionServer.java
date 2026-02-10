package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.servlet.component.romcom.protocol.MutationConflictResolution;
import cc.alcina.framework.servlet.component.romcom.protocol.Mutations;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.BeforeHandled;

public class MutationConflictResolutionServer
		extends MutationConflictResolution {
	@Override
	protected void onHasConflicts(BeforeHandled observable,
			ConflictingMutations conflictingMutations) {
		new MutationConflictResolution.Rejected((Mutations) observable.message,
				conflictingMutations).publish();
	}

	MutationConflictResolutionServer() {
		new TestRejectionObserver().bind();
		new RejectHappensAfterQueuedMutations().bind();
		new PerformPartialUndo().bind();
	}
}
