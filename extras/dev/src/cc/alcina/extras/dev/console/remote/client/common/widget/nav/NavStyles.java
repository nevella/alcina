package cc.alcina.extras.dev.console.remote.client.common.widget.nav;

import cc.alcina.framework.gwt.client.dirndl.StyleType;

public enum NavStyles implements StyleType {
	NAV_MODULE, BAR, CENTER, LOGO, SIGN_IN;
	public enum NavStylesCenter implements StyleType {
		MENU, SEARCH, ACTION, MENU_BUTTON, ACTION_BUTTON
	}

	public enum NavStylesCenterSearch implements StyleType {
	}

	public enum NavStylesPopup implements StyleType {
		NAV_SEARCH_POPUP, MENU, ITEM, LINE_2, LINE_1, SEPARATOR, SELECTED,
		POST_SEPARATOR;
	}
}
