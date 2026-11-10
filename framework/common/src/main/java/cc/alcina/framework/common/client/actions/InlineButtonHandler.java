package cc.alcina.framework.common.client.actions;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public abstract class InlineButtonHandler extends PermissibleAction
		implements ClickHandler {
	public InlineButtonHandler() {
	}

	@Override
	public String getCssClassName() {
		return "inline";
	}

	public static class InlineButtonHandlerAdapter extends InlineButtonHandler {
		private final String actionName;

		private final ClickHandler clickHandler;

		public InlineButtonHandlerAdapter(String actionName,
				ClickHandler clickHandler) {
			this.actionName = actionName;
			this.clickHandler = clickHandler;
		}

		@Override
		public String getActionName() {
			return actionName;
		}

		@Override
		public void onClick(ClickEvent event) {
			clickHandler.onClick(event);
		}
	}
}