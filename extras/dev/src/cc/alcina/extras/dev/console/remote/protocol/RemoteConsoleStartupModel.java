package cc.alcina.extras.dev.console.remote.protocol;

import cc.alcina.framework.common.client.csobjects.Bindable;

public class RemoteConsoleStartupModel extends Bindable {
	private String appName;

	public String getAppName() {
		return this.appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}
}
