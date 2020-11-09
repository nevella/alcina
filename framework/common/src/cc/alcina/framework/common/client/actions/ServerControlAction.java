package cc.alcina.framework.common.client.actions;

import java.io.Serializable;

import cc.alcina.framework.common.client.csobjects.ServerControlParams;

public class ServerControlAction
		extends RemoteActionWithParameters<ServerControlParams>
		implements Serializable {
	public ServerControlAction() {
		setParameters(new ServerControlParams());
	}

	@Override
	public String getDescription() {
		return "Various server control functions";
	}

	@Override
	public String getDisplayName() {
		return "Server control";
	}
}
