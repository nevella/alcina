package cc.alcina.framework.servlet.component.romcom.protocol;

import cc.alcina.framework.common.client.csobjects.Bindable;

public class RemoteComponentStartupModel extends Bindable {
	private String appName;

	public String getAppName() {
		return this.appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}
}
