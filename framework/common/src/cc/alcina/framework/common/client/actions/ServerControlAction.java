package cc.alcina.framework.common.client.actions;

import java.io.Serializable;

import cc.alcina.framework.common.client.csobjects.ServerControlParams;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;

public class ServerControlAction
		extends RemoteActionWithParameters<ServerControlParams>
		implements Serializable, SynchronousAction {
	public ServerControlAction() {
		setParameters(new ServerControlParams());
	}

	@Override
	public String getDescription() {
		return "Various server control functions";
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		return "Server control";
	}
}
