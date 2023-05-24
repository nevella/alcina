package cc.alcina.extras.dev.component.remote.protocol;

import cc.alcina.framework.common.client.csobjects.Bindable;

public class RemoteComponentResponse extends Bindable {
	private RemoteComponentStartupModel startupModel;

	private RemoteComponentConsoleChanges changes;

	public RemoteComponentConsoleChanges getChanges() {
		return this.changes;
	}

	public RemoteComponentStartupModel getStartupModel() {
		return startupModel;
	}

	public void setChanges(RemoteComponentConsoleChanges changes) {
		this.changes = changes;
	}

	public void setStartupModel(RemoteComponentStartupModel startupModel) {
		this.startupModel = startupModel;
	}
}
