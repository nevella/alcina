package cc.alcina.framework.gwt.client.module.support.login;

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

	private LoginModule() {
	}
}
