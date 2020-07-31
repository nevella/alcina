package cc.alcina.framework.gwt.client.lux;

import cc.alcina.framework.gwt.client.dirndl.StyleType;

public enum LuxStyle implements StyleType {
	MAIN_PANEL, LUX_SCREEN_CENTER, LUX;
	public enum LuxStyleHead implements StyleType {
		TITLE, SUBTITLE, LOGO
	}

	public enum LuxStyleModal implements StyleType {
		LUX_MODAL_PANEL, HEAD;
	}

	public enum LuxStyleStatus implements StyleType {
		LUX_STATUS_PANEL, ERROR, LOADING;
	}
}
