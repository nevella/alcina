package cc.alcina.framework.gwt.client.module.support.login;

import java.util.Collections;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.LoginResponseState;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.common.client.remote.LoginRemoteService;
import cc.alcina.framework.common.client.remote.LoginRemoteServiceAsync;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.EnumPlayer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.gwt.client.module.support.login.LoginConsort.State;
import cc.alcina.framework.gwt.client.module.support.login.pub.LoginActivity.LoginViewModel;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilderLight;

@RegistryLocation(registryPoint = LoginConsort.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
public abstract class LoginConsort extends Consort<State> {
	private SimplePanel container;

	protected LoginRequest request = new LoginRequest();

	@SuppressWarnings("unused")
	private LoginViewModel viewModel;

	Topic<Boolean> topicCallingRemote = Topic.local();

	Topic<String> topicMessage = Topic.local();

	protected LoginResponse lastResponse;

	public LoginConsort() {
	}

	public void init(SimplePanel container, LoginViewModel model) {
		this.container = container;
		this.viewModel = model;
		addPlayer(new Player_Got_username());
		addPlayer(new Player_Got_password());
		addPlayer(new Player_Got_2fa());
		addEndpointPlayer();
	}

	public void onClickNext(ClickEvent event) {
		/*
		 * TODO - remote action builder - promises
		 */
		topicCallingRemote.publish(true);
		getAsyncService().login(request, new AsyncCallback<LoginResponse>() {
			@Override
			public void onFailure(Throwable caught) {
				topicCallingRemote.publish(false);
				String message = Ax.blankTo(caught.getMessage(),
						() -> CommonUtils.toSimpleExceptionMessage(caught));
				topicMessage.publish(message);
			}

			@Override
			public void onSuccess(LoginResponse loginResponse) {
				lastResponse = loginResponse;
				topicCallingRemote.publish(false);
				handleSuccess(loginResponse);
			}
		});
	}

	public boolean shouldShowQrCode() {
		return lastResponse.getStates()
				.contains(LoginResponseState.Two_factor_qr_code_required);
	}

	protected Widget getLogo() {
		return null;
	}

	protected abstract String getTitleText();

	protected void handleSuccess(LoginResponse response) {
		boolean hasRequestUsername = Ax.notBlank(request.getUserName());
		if (!hasRequestUsername) {
			clearReachedStates();
			nudge();
			return;//
		}
		if (Ax.notBlank(response.getErrorMsg())) {
			topicMessage.publish(response.getErrorMsg());
			return;
		}
		boolean hasRequestPassword = Ax.notBlank(request.getPassword());
		if (response.getStates()
				.contains(LoginResponseState.Username_not_found)) {
			handleUsernameNotFound();
			return;
		}
		addState(State.Got_username);
		if (!hasRequestPassword) {
			wasPlayed(playing.get(0));
			return;
		}
		if (response.getStates().contains(LoginResponseState.Password_incorrect)
				|| response.getStates()
						.contains(LoginResponseState.Invalid_credentials)) {
			return;
		}
		addState(State.Got_password);
		if (response.getStates()
				.contains(LoginResponseState.Two_factor_code_required)) {
			wasPlayed(playing.get(0));
			return;
		}
		if (response.getStates()
				.contains(LoginResponseState.Account_cannot_login)) {
			return;
		}
		wasPlayed(playing.get(0), Collections.singleton(State.Got_2fa_code));
	}

	protected void handleUsernameNotFound() {
		// for subclasses, e.g. show a 'sign up' dialog
	}

	LoginRemoteServiceAsync getAsyncService() {
		LoginRemoteServiceAsync service = (LoginRemoteServiceAsync) GWT
				.create(LoginRemoteService.class);
		Registry.impl(AlcinaRpcRequestBuilderLight.class)
				.adjustEndpoint((ServiceDefTarget) service);
		return service;
	}

	class Player_Got_2fa extends EnumPlayer<State> {
		public Player_Got_2fa() {
			super(State.Got_2fa_code);
			setAsynchronous(true);
		}

		@Override
		public void run() {
			container.setWidget(new LoginPage2FA(LoginConsort.this));
		}
	}

	class Player_Got_password extends EnumPlayer<State> {
		public Player_Got_password() {
			super(State.Got_password);
			setAsynchronous(true);
		}

		@Override
		public void run() {
			container.setWidget(new LoginPagePassword(LoginConsort.this));
		}
	}

	class Player_Got_username extends EnumPlayer<State> {
		public Player_Got_username() {
			super(State.Got_username);
			setAsynchronous(true);
		}

		@Override
		public void run() {
			container.setWidget(new LoginPageUsername(LoginConsort.this));
		}
	}

	enum State {
		Got_username, Got_password, Got_2fa_code
	}
}
