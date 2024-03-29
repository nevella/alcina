package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Reflected
@Registration.Singleton
public class ClientState extends Bindable {
	public static ClientState get() {
		return Registry.impl(ClientState.class);
	}

	private boolean uiInitialised;

	private boolean appReadOnly;

	public boolean isAppReadOnly() {
		return this.appReadOnly;
	}

	public boolean isUiInitialised() {
		return this.uiInitialised;
	}

	public void setAppReadOnly(boolean appReadOnly) {
		this.appReadOnly = appReadOnly;
	}

	public void setUiInitialised(boolean uiInitialised) {
		boolean old_uiInitialised = this.uiInitialised;
		this.uiInitialised = uiInitialised;
		propertyChangeSupport().firePropertyChange("uiInitialised",
				old_uiInitialised, uiInitialised);
	}
}
