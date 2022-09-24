package cc.alcina.framework.common.client.actions;

import java.io.Serializable;

import cc.alcina.framework.common.client.csobjects.RemoteDebugParams;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;

public class RemoteDebugAction
		extends RemoteActionWithParameters<RemoteDebugParams>
		implements Serializable, SynchronousAction,PreserveHistoryAction {
	public RemoteDebugAction() {
		setParameters(new RemoteDebugParams());
	}

	@Override
	public String getDescription() {
		return "Execute debug commands";
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		return "Remote console";
	}
}
