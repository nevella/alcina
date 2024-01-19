package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * Abstracts services available to the host environment (e.g. the local desktop
 * if running in a bare-metal jdk)
 */
public class VmEnvironment {
	public interface BrowserAccess {
		public static BrowserAccess get() {
			return Registry.impl(BrowserAccess.class);
		}

		void openUrl(String url);
	}
}
