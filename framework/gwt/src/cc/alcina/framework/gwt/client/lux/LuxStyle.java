package cc.alcina.framework.gwt.client.lux;

public enum LuxStyle implements LuxStyleType {
	MAIN_PANEL, LUX_SCREEN_CENTER,LUX;
	public enum LuxStyleHead implements LuxStyleType {
		TITLE, SUBTITLE, LOGO
	}

	public enum LuxStyleModal implements LuxStyleType {
		LUX_MODAL_PANEL, HEAD;
	}
	public enum LuxStyleStatus implements LuxStyleType {
		 LUX_STATUS_PANEL,ERROR,LOADING;
	}
}
