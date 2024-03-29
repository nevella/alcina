package cc.alcina.extras.dev.console.remote.protocol;

import cc.alcina.framework.common.client.csobjects.Bindable;

public class RemoteConsoleResponse extends Bindable {
	private RemoteConsoleStartupModel startupModel;

	private RemoteConsoleConsoleChanges changes;

	public RemoteConsoleConsoleChanges getChanges() {
		return this.changes;
	}

	public RemoteConsoleStartupModel getStartupModel() {
		return startupModel;
	}

	public void setChanges(RemoteConsoleConsoleChanges changes) {
		this.changes = changes;
	}

	public void setStartupModel(RemoteConsoleStartupModel startupModel) {
		this.startupModel = startupModel;
	}
}
