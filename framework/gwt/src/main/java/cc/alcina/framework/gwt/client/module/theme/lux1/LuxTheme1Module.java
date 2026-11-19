package cc.alcina.framework.gwt.client.module.theme.lux1;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.gwt.client.lux.LuxModule;

public class LuxTheme1Module {
	private static LuxTheme1Module module;

	public static void ensure() {
		if (module == null) {
			module = new LuxTheme1Module();
		}
	}

	static LuxTheme1Module get() {
		ensure();
		return module;
	}

	public LuxTheme1Resources resources = GWT.create(LuxTheme1Resources.class);

	private LuxTheme1Module() {
		LuxModule.get().interpolateAndInject(resources.luxFormStylesText());
		LuxModule.get().interpolateAndInject(resources.luxFormStylesRadio());
		LuxModule.get().interpolateAndInject(resources.luxFormStylesSelector());
		LuxModule.get().interpolateAndInject(resources.luxButtonStyles());
		LuxModule.get().interpolateAndInject(resources.luxFormStyles());
		LuxModule.get().interpolateAndInject(resources.luxModalPanelStyles());
		LuxModule.get().interpolateAndInject(resources.luxStatusPanelStyles());
	}
}
