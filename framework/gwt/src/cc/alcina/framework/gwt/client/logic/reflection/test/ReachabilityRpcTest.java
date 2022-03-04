package cc.alcina.framework.gwt.client.logic.reflection.test;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.csobjects.LoadObjectsResponse;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.common.client.remote.ReflectiveRemoteServiceAsync;

public class ReachabilityRpcTest {
	public interface Int1 {
		void out();
	}

	public interface Int2 {
		void out();
	}

	@Reflected
	@Registration.Singleton
	public static class ReflectiveLoginRemoteServiceAsync2
			extends ReflectiveRemoteServiceAsync {
		public static ReflectiveLoginRemoteServiceAsync2 get() {
			return Registry.impl(ReflectiveLoginRemoteServiceAsync2.class);
		}

		public void loadInitial(LoadObjectsRequest request,
				AsyncCallback<LoadObjectsResponse> callback) {
			RootPanel.get().add(new Label("asdfasdf chirp"));
		}

		public void login(LoginRequest request,
				AsyncCallback<LoginResponse> callback) {
			RootPanel.get().add(new Label("asdfasdf chirp2"));
		}
	}

	public static class Reg1 {
	}

	@Registration({ Int2.class, Reg1.class })
	@Reflected
	public static class Reg2 implements Int1 {
		@Override
		public void out() {
			RootPanel.get().add(new Label("asdfasdf wore"));
		}
	}
}
