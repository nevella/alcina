package cc.alcina.framework.common.client.actions;

import java.io.Serializable;

import cc.alcina.framework.common.client.csobjects.RemoteDebugParams;

public class RemoteDebugAction
		extends RemoteActionWithParameters<RemoteDebugParams>
		implements Serializable, SynchronousAction, PreserveHistoryAction {
	public RemoteDebugAction() {
		setParameters(new RemoteDebugParams());
	}

	@Override
	public String getActionName() {
		return "Remote console";
	}

	@Override
	public String getDescription() {
		return "Execute debug commands";
	}
}
