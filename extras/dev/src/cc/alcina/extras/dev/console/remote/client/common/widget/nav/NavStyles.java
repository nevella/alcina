package cc.alcina.extras.dev.console.remote.client.common.widget.nav;

import cc.alcina.framework.gwt.client.lux.LuxStylesType;

public enum NavStyles implements LuxStylesType {
	NAV_MODULE, BAR, CENTER, LOGO, SIGN_IN;
	public enum NavStylesCenter implements LuxStylesType {
		MENU, SEARCH, ACTION, MENU_BUTTON, ACTION_BUTTON
	}

	public enum NavStylesCenterSearch implements LuxStylesType {
	}

	public enum NavStylesPopup implements LuxStylesType {
		NAV_SEARCH_POPUP, MENU, ITEM, LINE_2, LINE_1, SEPARATOR, SELECTED,
		POST_SEPARATOR;
	}
}
