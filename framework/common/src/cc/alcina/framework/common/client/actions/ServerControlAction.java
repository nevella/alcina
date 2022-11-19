package cc.alcina.framework.common.client.actions;

import java.io.Serializable;

import cc.alcina.framework.common.client.csobjects.ServerControlParams;

public class ServerControlAction
		extends RemoteActionWithParameters<ServerControlParams>
		implements Serializable, SynchronousAction {
	public ServerControlAction() {
		setParameters(new ServerControlParams());
	}

	@Override
	public String getActionName() {
		return "Server control";
	}

	@Override
	public String getDescription() {
		return "Various server control functions";
	}
}
