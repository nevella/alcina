package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.state.MachineModel;

public class ClientConfigurationModel extends MachineModel{
	private boolean startupCancelled;

	public boolean isStartupCancelled() {
		return this.startupCancelled;
	}

	public void setStartupCancelled(boolean startupCancelled) {
		this.startupCancelled = startupCancelled;
	}
}
