package cc.alcina.framework.gwt.client.module.theme.dirndl.lux1;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;

public class LuxDirndlTheme1Module {
	private static LuxDirndlTheme1Module module;

	public static void ensure() {
		if (module == null) {
			module = new LuxDirndlTheme1Module();
		}
	}

	static LuxDirndlTheme1Module get() {
		ensure();
		return module;
	}

	public LuxDirndlTheme1Resources resources = GWT
			.create(LuxDirndlTheme1Resources.class);

	private LuxDirndlTheme1Module() {
		StyleInjector.injectAtEnd(resources.styles().getText());
	}
}
