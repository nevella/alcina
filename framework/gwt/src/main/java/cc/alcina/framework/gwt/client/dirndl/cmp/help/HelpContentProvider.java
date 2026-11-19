package cc.alcina.framework.gwt.client.dirndl.cmp.help;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.EnvironmentRegistration
public interface HelpContentProvider {
	public static HelpContentProvider get() {
		return Registry.impl(HelpContentProvider.class);
	}

	String getHelpMarkup();
}
