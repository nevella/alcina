package cc.alcina.framework.common.client.actions;


import com.google.gwt.event.dom.client.ClickHandler;

public abstract class InlineButtonHandler extends PermissibleAction implements
		ClickHandler {
	@Override
	public String getCssClassName() {
		return "inline";
	}
}