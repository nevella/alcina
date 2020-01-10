package cc.alcina.framework.gwt.client.module.support.login;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.gwt.client.lux.LuxModule;
import cc.alcina.framework.gwt.client.module.theme.lux1.LuxTheme1Module;

public class LoginModule {
	private static LoginModule indexModule;

	public static void ensure() {
		if (indexModule == null) {
			indexModule = new LoginModule();
		}
	}

	public static void focusNavbarSearch() {
		ensure();
	}

	public LoginResources resources = GWT.create(LoginResources.class);

	private LoginModule() {
		LuxTheme1Module.ensure();
		LuxModule.get().interpolateAndInject(resources.loginStyles());
	}
}
