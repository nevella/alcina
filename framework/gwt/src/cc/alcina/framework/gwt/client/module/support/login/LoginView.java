package cc.alcina.framework.gwt.client.module.support.login;

import java.beans.PropertyChangeEvent;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.SimplePanel;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.entity.view.AbstractViewModelView;
import cc.alcina.framework.gwt.client.entity.view.ViewModelView;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsort;
import cc.alcina.framework.gwt.client.lux.LuxStyle;
import cc.alcina.framework.gwt.client.module.support.login.pub.LoginActivity.LoginViewModel;

//FIXME - make loading async
@RegistryLocation(registryPoint = ViewModelView.class, targetClass = LoginViewModel.class)
@ClientInstantiable
public class LoginView extends AbstractViewModelView<LoginViewModel> {
	private SimplePanel panel;

	private LoginConsort loginConsort;

	public LoginView() {
		this.panel = new SimplePanel();
		initWidget(panel);
		LoginModule.ensure();
		LuxStyle.LUX_SCREEN_CENTER.addTo(panel);
		render();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	}

	@Override
	public void setModel(LoginViewModel model) {
		if (PermissionsManager.get().isLoggedIn()) {
			History.newItem("");
			return;
		}
		this.loginConsort = Registry.impl(LoginConsort.class);
		this.loginConsort.init(panel, model);
		loginConsort.exitListenerDelta((k, v) -> {
			if (v instanceof Throwable) {
			} else {
				Registry.impl(HandshakeConsort.class)
						.handleLoggedIn(loginConsort.lastResponse);
			}
		}, false, true);
		loginConsort.start();
	}

	private void render() {
	}
}
