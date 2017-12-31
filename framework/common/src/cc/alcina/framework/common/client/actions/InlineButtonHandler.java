package cc.alcina.framework.common.client.actions;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public abstract class InlineButtonHandler extends PermissibleAction
		implements ClickHandler {
	public InlineButtonHandler() {
	}

	public InlineButtonHandler(String displayName, String actionName) {
		super(displayName, actionName);
	}

	@Override
	public String getCssClassName() {
		return "inline";
	}

	public static class InlineButtonHandlerAdapter extends InlineButtonHandler {
		private final ClickHandler clickHandler;

		public InlineButtonHandlerAdapter(String displayName,
				ClickHandler clickHandler) {
			super(displayName, null);
			this.clickHandler = clickHandler;
		}

		@Override
		public void onClick(ClickEvent event) {
			clickHandler.onClick(event);
		}
	}
}