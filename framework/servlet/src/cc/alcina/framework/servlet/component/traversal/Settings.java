package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class Settings extends Bindable.Fields {
	public static Settings get() {
		return Registry.impl(Settings.class);
	}

	public boolean showContainerLayers = false;
}
