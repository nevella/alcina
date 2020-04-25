package cc.alcina.framework.gwt.client.module.support.login.pub;

import cc.alcina.framework.gwt.client.entity.view.TypedModelActivity;
import cc.alcina.framework.gwt.client.entity.view.ViewModel;
import cc.alcina.framework.gwt.client.module.support.login.pub.LoginActivity.LoginViewModel;

public class LoginActivity
		extends TypedModelActivity<LoginPlace, LoginViewModel> {
	public LoginActivity(LoginPlace place) {
		super(place);
	}

	@Override
	protected Class<LoginViewModel> getModelClass() {
		return LoginViewModel.class;
	}

	public static class LoginViewModel extends ViewModel<LoginPlace> {
		public LoginViewModel() {
		}
	}
}
