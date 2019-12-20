package cc.alcina.framework.gwt.client.module.theme.lux1;

import com.google.gwt.resources.client.ClientBundle;

import cc.alcina.framework.gwt.client.gen.SimpleCssResource;

public interface LuxTheme1Resources extends ClientBundle {
	@Source("res/lux-button-styles.css")
	public SimpleCssResource luxButtonStyles();

	@Source("res/lux-form-styles.css")
	public SimpleCssResource luxFormStyles();

	@Source("res/lux-form-styles-checkbox.css")
	public SimpleCssResource luxFormStylesCheckbox();

	@Source("res/lux-form-styles-radio.css")
	public SimpleCssResource luxFormStylesRadio();

	@Source("res/lux-form-styles-selector.css")
	public SimpleCssResource luxFormStylesSelector();

	@Source("res/lux-form-styles-text.css")
	public SimpleCssResource luxFormStylesText();
	
	@Source("res/lux-modal-panel-styles.css")
	public SimpleCssResource luxModalPanelStyles();
}
