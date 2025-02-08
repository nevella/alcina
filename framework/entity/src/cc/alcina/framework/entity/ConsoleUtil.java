package cc.alcina.framework.entity;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class ConsoleUtil {
	public static void copyToClipboard(String s) {
		Registry.impl(CopyToClipboard.class).copy(s);
	}

	@Registration.Self
	public static abstract class CopyToClipboard {
		public abstract void copy(String s);
	}
}
