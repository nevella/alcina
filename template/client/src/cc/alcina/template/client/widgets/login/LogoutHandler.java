package cc.alcina.template.client.widgets.login;



import cc.alcina.framework.common.client.logic.permissions.LoginStateVisibleWithWidget;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsort;
import cc.alcina.framework.gwt.client.widget.Link;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

public class LogoutHandler implements ClickHandler,
		LoginStateVisibleWithWidget {
	private Link hyperlink;

	public LogoutHandler() {
		this.hyperlink = new Link("Log out", true);
		hyperlink.addClickHandler(this);
	}

	public Widget getWidget() {
		return this.hyperlink;
	}

	public void onClick(ClickEvent clickEvent) {
		Registry.impl(HandshakeConsort.class).logout();
	}

	public boolean visibleForLoginState(LoginState state) {
		return state == LoginState.LOGGED_IN;
	}
	public String getDebugId() {
		return AlcinaDebugIds.getButtonId(AlcinaDebugIds.TOP_BUTTON_LOGOUT);
	}
}
