package cc.alcina.template.client.widgets.login;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.permissions.LoginStateVisibleWithWidget;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsort;
import cc.alcina.framework.gwt.client.widget.APanel;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.dialog.LoginDisplayer;
import cc.alcina.template.client.logic.AlcinaTemplateContentProvider;
import cc.alcina.template.client.widgets.AlcinaTemplateLayoutManager;
import cc.alcina.template.cs.AlcinaTemplateHistoryItem;
import cc.alcina.template.cs.remote.AlcinaTemplateRemoteServiceAsync;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

public class LoginHandler implements LoginStateVisibleWithWidget, ClickHandler,
		PermissibleActionListener {
	private Link hyperlink;

	private LoginDisplayer ld;

	public LoginHandler() {
		this.hyperlink = new Link("Login");
		hyperlink.addClickHandler(this);
	}

	public String getDebugId() {
		return AlcinaDebugIds.getButtonId(AlcinaDebugIds.TOP_BUTTON_LOGIN);
	}

	public Widget getProblemHandlerWidget(final String userName) {
		APanel panel = new APanel();
		panel.add(new InlineLabel("Problems with login?"));
		panel.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent clickEvent) {
				AlcinaTemplateHistoryItem info = new AlcinaTemplateHistoryItem();
				info.setContentToken(AlcinaTemplateContentProvider.PROBLEMS_LOGGING_IN);
				info.setNoHistory(true);
				// ResetPasswordHandler.username = userName;
				ld.hideLoginDialog();
				AlcinaTemplateLayoutManager.get().getMainCmp()
						.onHistoryChanged(info.toTokenString());
			}
		});
		return panel;
	}

	public Widget getWidget() {
		return this.hyperlink;
	}

	public void onClick(ClickEvent clickEvent) {
		ld = new LoginDisplayer();
		ld.showLoginDialog(this);
	}

	public void vetoableAction(PermissibleActionEvent evt) {
		if (evt.getAction().getActionName() == LoginDisplayer.LOGIN_ACTION) {
			ld.enableLoginButtons(false);
			final LoginBean lb = (LoginBean) evt.getParameters();
			AsyncCallback<LoginResponse> callback = new AsyncCallback<LoginResponse>() {
				public void onFailure(Throwable caught) {
					LoginResponse lrb = new LoginResponse();
					lrb.setErrorMsg(caught.getMessage());
					cleanup(lrb);
				}

				public void onSuccess(LoginResponse result) {
					cleanup(result);
				}

				void cleanup(LoginResponse lrb) {
					if (!lrb.isOk()) {
						ld.enableLoginButtons(true);
						ld.setStatus(lrb.getErrorMsg());
						ld.getStatusLabel().setStyleName("login-error");
						ld.setProblemHandlerWidget(getProblemHandlerWidget(lb
								.getUserName()));
					} else {
						ld.hideLoginDialog();
						Registry.impl(HandshakeConsort.class).handleLoggedIn(
								lrb);
					}
				}
			};
			Registry.impl(AlcinaTemplateRemoteServiceAsync.class).login(lb,
					callback);
		}
		if (evt.getAction().getActionName() == LoginDisplayer.CANCEL_ACTION) {
			ld.hideLoginDialog();
		}
	}

	public boolean visibleForLoginState(LoginState state) {
		return state == LoginState.NOT_LOGGED_IN;
	}
}
