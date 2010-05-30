package cc.alcina.template.client.widgets.login;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import cc.alcina.framework.common.client.logic.permissions.LoginStateVisibleWithWidget;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class LoginStatus implements  
		PropertyChangeListener, LoginStateVisibleWithWidget {
	private Label label;

	public LoginStatus() {
		label = new Label("") {
			@Override
			protected void onDetach() {
				PermissionsManager.get().removePropertyChangeListener(
						LoginStatus.this);
				AlcinaTemplateObjects.current().getCurrentUser()
						.removePropertyChangeListener(LoginStatus.this);
				super.onDetach();
			}
		};
		PermissionsManager.get().addPropertyChangeListener(this);
		AlcinaTemplateObjects.current().getCurrentUser().addPropertyChangeListener(this);
		valueChanged();
	}

	

	public Widget getWidget() {
		return this.label;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		valueChanged();
	}

	public void stateChanged(Object source, String newState) {
		valueChanged();
	}

	void valueChanged() {
		if (PermissionsManager.get().isLoggedIn()) {
			label.setText("Logged in as "
					+ AlcinaTemplateObjects.current().getCurrentUser().friendlyName());
		} else {
			label.setText("Not logged in");
		}
	}

	public boolean visibleForLoginState(LoginState state) {
		return state == LoginState.LOGGED_IN;
	}
	public String getDebugId() {
		return AlcinaDebugIds.getButtonId(AlcinaDebugIds.TOP_BUTTON_LOGIN_STATUS);
	}
}
