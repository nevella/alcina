package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class Environment {
	public interface BrowserAccess {
		public static BrowserAccess get() {
			return Registry.impl(BrowserAccess.class);
		}

		void openUrl(String url);
	}
}
