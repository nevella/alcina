package cc.alcina.extras.dev.console.remote.client.common.widget.nav;

import cc.alcina.framework.gwt.client.lux.LuxStyleType;

public enum NavStyles implements LuxStyleType {
	NAV_MODULE, BAR, CENTER, LOGO, SIGN_IN;
	public enum NavStylesCenter implements LuxStyleType {
		MENU, SEARCH, ACTION, MENU_BUTTON, ACTION_BUTTON
	}

	public enum NavStylesCenterSearch implements LuxStyleType {
	}

	public enum NavStylesPopup implements LuxStyleType {
		NAV_SEARCH_POPUP, MENU, ITEM, LINE_2, LINE_1, SEPARATOR, SELECTED,
		POST_SEPARATOR;
	}
}
