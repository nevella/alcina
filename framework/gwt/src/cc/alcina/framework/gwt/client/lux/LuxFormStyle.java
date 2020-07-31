package cc.alcina.framework.gwt.client.lux;

import cc.alcina.framework.gwt.client.dirndl.StyleType;

public enum LuxFormStyle implements StyleType {
	LUX_FORM, LUX_FORM_LABEL, LUX_FORM_ELEMENT, LUX_FORM_LABEL_CONTAINER,
	NO_LABEL, RADIO, RADIO_GROUP, TEXT, SELECTOR, LUX_FORM_CONTAINER,
	LUX_FORM_FEEDBACK;
	public static final String NO_LABEL_RADIO_GROUP_S = "radio-group no-label";

	public static final String REQUIRED_S = "required";

	public static final String LUX_FORM_ELEMENT_RADIO_S = "lux-form-element radio";
}